package com.yanfei;


import java.util.List;

public class Main {
    private static String regions = "[{\"province\":{\"id\":\"320000\",\"name\":\"江苏\",\"selectAll\":true}},{\"province\":{\"id\":\"360000\",\"name\":\"江西\",\"selectAll\":true}},{\"province\":{\"id\":\"420000\",\"name\":\"湖北\",\"selectAll\":true}}]";


    public static void main(String[] args) {
        System.out.print("Hello world");
    }

    private List<ItemDeliverRegion.ProvinceRegion> provinceJsonToObject(String provinceJsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        JavaType pRegionListType = mapper.createCollectionType(List.class, ItemDeliverRegion.ProvinceRegion.class);
        /**
         * 构造provinceRegions对象,生成json字符串;用来对比前台传过来的json串格式是否正确
         */
        constructProvinceList();
        String provinceJsonStr = JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(provinceRegionConstructs);
        List<ItemDeliverRegion.ProvinceRegion> provinceList = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(provinceJsonStr, pRegionListType);

        /**
         * 前台传过来的字符串,生成的json对象
         */
        provinceList = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(deliverTemplate.getRegions(), pRegionListType);

        return provinceList;
    }
}
