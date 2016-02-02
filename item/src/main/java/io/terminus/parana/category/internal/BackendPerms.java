/*
 * Copyright (c) 2015 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.internal;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;
import io.terminus.common.utils.Splitters;
import io.terminus.parana.category.dao.mysql.BackCategoryPermDao;
import io.terminus.parana.category.model.BackCategoryPerm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Effet
 */
@Slf4j
@Component
public class BackendPerms {

    @Autowired
    private BackCategoryPermDao backCategoryPermDao;

    private static Function<String, Set<Long>> toSet = new Function<String, Set<Long>>() {
        @Override
        public Set<Long> apply(@Nullable String input) {
            return ImmutableSet.copyOf(Lists.transform(
                    Splitters.COMMA.splitToList(MoreObjects.firstNonNull(input, "")),
                    Longs.stringConverter()));
        }
    };

    public class Perm {
        private Map<Long, Integer> treeAllow;
        private Set<Long> brandAllow;

        private Perm(Map<Long, Integer> treeAllow, Set<Long> brandAllow) {
            this.treeAllow = treeAllow;
            this.brandAllow = brandAllow;
        }

        public boolean checkPathPerm(List<Long> path) {
            checkArgument(path != null && !path.isEmpty(), "path must not null nor empty");
            checkArgument(path.get(0) == 0, "path must start from 0-node (root)");
            for (Long id : path) {
                Integer color = treeAllow.get(checkNotNull(id, "null exist in path"));
                if (color == null || color == 0) {
                    return false;
                }
                if (color == 2) {
                    return true;
                }
            }
            return treeAllow.get(path.get(path.size() - 1)) == 1;
        }

        public boolean checkBrandPerm(Set<Long> brands) {
            checkArgument(brands != null && !brands.isEmpty(), "brands must not null nor empty");
            return brandAllow.contains(0L) || brandAllow.containsAll(brands);
        }

        public boolean checkBrandPerm(Long... brands) {
            checkArgument(brands != null && brands.length > 0, "brands must not null nor empty");
            return brandAllow.contains(0L) || brandAllow.containsAll(Sets.newHashSet(brands));
        }

        public Map<Long, Integer> getTreeAllow() {
            return treeAllow;
        }
    }

    public Perm buildPerm(Long userId, Integer userType) {
        checkArgument(userId != null && userType != null, "userId and userType must not null");

        Optional<BackCategoryPerm> permOpt = backCategoryPermDao.findOne(userId, userType);
        if (!permOpt.isPresent()) {
            // 查询所有此类用户默认权限
            permOpt = backCategoryPermDao.findOne(-1l, userType);
        }
        checkState(permOpt.isPresent(), "user id=%s, type=%s haven't perm data",
                userId, userType);

        List<String> rawPerm = Splitters.COMMA.splitToList(permOpt.get().getAllow());
        Map<Long, Integer> treeAllow = new HashMap<>();
        for (String line : rawPerm) {
            Integer color = 2;
            if (line.startsWith("h") || line.startsWith("H")) {
                line = line.substring(1);
                color = 1;
            }
            treeAllow.put(Long.parseLong(line), color);
        }

        Set<Long> brandAllow = toSet.apply(permOpt.get().getBrandAllow());

        return new Perm(treeAllow, brandAllow);
    }
}
