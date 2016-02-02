/*
 * Copyright (c) 2015 杭州端点网络科技有限公司
 */

package io.terminus.parana.category.internal;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import io.terminus.parana.category.cache.BaseCacher;
import io.terminus.parana.category.dao.mysql.BackCategoryDao;
import io.terminus.parana.category.dao.mysql.BaseCategoryDao;
import io.terminus.parana.category.dao.mysql.FrontCategoryDao;
import io.terminus.parana.category.dto.RichCategory;
import io.terminus.parana.category.dto.Tree;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.category.model.BaseCategory;
import io.terminus.parana.category.model.FrontCategory;
import io.terminus.parana.common.util.Iters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Effet
 */
@Slf4j
@Component
public class CategoryQueries {

    @Autowired
    private BackCategoryDao backCategoryDao;

    @Autowired
    private FrontCategoryDao frontCategoryDao;

    @Autowired
    private BaseCacher categoryCacher;

    public <C extends BaseCategory, D extends BaseCategoryDao<C>> List<Long> ancestorPath(Long id, D dao) {
        checkArgument(id != null && id >= 0, "illegal category id");

        List<Long> path = Lists.newArrayList(id);

        for (int deep = 10; deep > 0; --deep) {
            Long cid = Iterables.getLast(path);
            if (cid == 0) break;

            C category = checkNotNull(dao.load(cid), "category not found, id=%s", cid);
            Long pid = checkNotNull(category.getPid(), "category pid is null, id=%s", cid);
            path.add(pid);
        }
        path = Lists.reverse(path);
        checkState(path.get(0) == 0, "path must start from 0-node (root)");

        return path;
    }

    public <C extends BackCategory, D extends BaseCategoryDao<C>> Set<Long> children(Long id, D dao) {
        checkArgument(id != null && id >= 0, "illegal category id");

        List<C> categories = checkNotNull(dao.findByPid(id), "categories null by pid=%s", id);

        return FluentIterable.from(categories)
                .transform(new Function<C, Long>() {
                    @Nullable
                    @Override
                    public Long apply(@Nullable C input) {
                        return input == null ? null : input.getId();
                    }
                })
                .filter(Predicates.notNull())
                .toSet();
    }

    private BackCategory load(long id, boolean cache) {
        if (cache) {
            return categoryCacher.findBackCategoryById(id);
        } else {
            return backCategoryDao.load(id);
        }
    }

    private List<BackCategory> findByPid(long id, boolean cache) {
        List<BackCategory> result;
        if (cache) {
            RichCategory<BackCategory> children = categoryCacher.findChildrenByBackCategoryId(id);
            result = children != null ? children.getChildren() : null;
        } else {
            result = backCategoryDao.findByPid(id);
        }

        return Iters.nullToEmpty(result);
    }

    private FrontCategory loadF(long id, boolean cache) {
        if (cache) {
            return categoryCacher.findFrontCategoryById(id);
        } else {
            return frontCategoryDao.load(id);
        }
    }

    private List<FrontCategory> findFByPid(long id, boolean cache) {
        List<FrontCategory> result;
        if (cache) {
            RichCategory<FrontCategory> children = categoryCacher.findChildrenByFrontCategoryId(id);
            result = children != null ? children.getChildren() : null;
        } else {
            result = frontCategoryDao.findByPid(id);
        }

        return Iters.nullToEmpty(result);
    }

    private List<FrontCategory> findFInPids(List<Long> ids, boolean cache) {
        return frontCategoryDao.findInPids(ids);
    }

    public Tree<BackCategory> backTree(long rootId, int deep, boolean cache) {
        if (deep == 0) return null;

        BackCategory root;
        if (rootId == 0) {
            root = new BackCategory();
            root.setId(0l);
            root.setPid(0l);
            root.setLevel(0);
            root.setHasChildren(Boolean.TRUE);
        } else {
            root = load(rootId, cache);
            if (root == null) {
                return null;
            }
        }
        List<BackCategory> bcs = findByPid(rootId, cache);
        List<Tree<BackCategory>> children = Lists.newArrayList();

        for (BackCategory bc : bcs) {
            Tree<BackCategory> rc = backTree(bc.getId(), deep - 1, cache);
            if (rc != null) children.add(rc);
        }

        Tree<BackCategory> result = new Tree<>();
        result.setNode(root);
        result.setChildren(children);
        return result;
    }

    @Deprecated
    public Tree<FrontCategory> frontTree(long rootId, int deep, boolean cache) {
        if (deep == 0) {
            return null;
        }


        FrontCategory root;

        if (rootId == 0) {
            root = new FrontCategory();
            root.setId(0l);
            root.setPid(0l);
            root.setLevel(0);
            root.setHasChildren(Boolean.TRUE);
        } else {
            root = loadF(rootId, cache);
            if (root == null) {
                return null;
            }
        }
        List<FrontCategory> fcs = findFByPid(rootId, cache);
        List<Tree<FrontCategory>> children = Lists.newArrayList();

        for (FrontCategory fc : fcs) {
            Tree<FrontCategory> rc = frontTree(fc.getId(), deep - 1, cache);
            if (rc != null) {
                children.add(rc);
            }
        }

        Tree<FrontCategory> result = new Tree<>();
        result.setNode(root);
        result.setChildren(children);
        return result;
    }

    public Tree<FrontCategory> getFrontTree(long id, int deep, boolean cache) {
        FrontCategory root;
        if (id == 0) {
            root = new FrontCategory();
            root.setId(0l);
            root.setPid(0l);
            root.setLevel(0);
            root.setHasChildren(Boolean.TRUE);
        } else {
            root = loadF(id, cache);
            if (root == null) {
                return null;
            }
        }

        Tree<FrontCategory> rootNode = new Tree<>();
        rootNode.setNode(root);
        rootNode.setChildren(new ArrayList<Tree<FrontCategory>>());

        Vector<Vector<Tree<FrontCategory>>> row = new Vector<>(2);
        row.add(0, new Vector<Tree<FrontCategory>>());
        row.add(1, new Vector<Tree<FrontCategory>>());

        int initLevel = root.getLevel() + 1;
        int curLevel = initLevel;
        row.get(curLevel % 2).add(rootNode);
        while (curLevel - initLevel < deep - 1 && !row.get(curLevel % 2).isEmpty()) {
            row.get((curLevel + 1) % 2).setSize(0); // empty next row
            Map<Long, Tree<FrontCategory>> view = new HashMap<>();
            for (Tree<FrontCategory> fc : row.get(curLevel % 2)) {
                view.put(fc.getNode().getId(), fc);
            }
            List<FrontCategory> fcs = findFInPids(ImmutableList.copyOf(view.keySet()), cache);
            for (FrontCategory fc : fcs) {
                Tree<FrontCategory> fcNode = new Tree<>();
                fcNode.setNode(fc);
                fcNode.setChildren(new ArrayList<Tree<FrontCategory>>());
                view.get(fc.getPid()).getChildren().add(fcNode);
                row.get((curLevel + 1) % 2).add(fcNode);
            }
            ++ curLevel;
        }

        return rootNode;
    }

}
