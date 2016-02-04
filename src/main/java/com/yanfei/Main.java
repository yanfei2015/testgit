package com.yanfei;


import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * git mytest branch commit 1
 */

/**
 * git mytest branch commit 2
 */
public class Main {
    /**
     * [{"province":{"id":530000,"name":"云南","selectAll":true}},{"province":{"id":330000,"name":"浙江","selectAll":false},"cities":[{"city":{"id":330001,"name":"杭州","selectAll":true},"regions":null},{"city":{"id":331100,"name":"丽水","selectAll":true},"regions":null},{"city":{"id":330500,"name":"湖州","selectAll":false},"regions":[{"id":330523,"name":"安吉"}]}]}]
     */
    private static String regions = "[{\"province\":{\"id\":530000,\"name\":\"云南\",\"selectAll\":true}},{\"province\":{\"id\":330000,\"name\":\"浙江\",\"selectAll\":false},\"cities\":[{\"city\":{\"id\":330001,\"name\":\"杭州\",\"selectAll\":true},\"regions\":null},{\"city\":{\"id\":331100,\"name\":\"丽水\",\"selectAll\":true},\"regions\":null},{\"city\":{\"id\":330500,\"name\":\"湖州\",\"selectAll\":false},\"regions\":[{\"id\":330523,\"name\":\"安吉\"}]}]}]";
    private static List<ItemDeliverRegion.ProvinceRegion> provinceRegions = new ArrayList<ItemDeliverRegion.ProvinceRegion>();
    private static List<ItemDeliverRegion.ProvinceRegion> provinceRegionConstructs = new ArrayList<ItemDeliverRegion.ProvinceRegion>();

    public static void main(String[] args) {
        List<ItemDeliverRegion.ProvinceRegion> provinceList = provinceJsonToObject(regions);
        System.out.print("Hello world");
    }

    public static List<ItemDeliverRegion.ProvinceRegion> provinceJsonToObject(String provinceJsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        JavaType pRegionListType = mapper.getTypeFactory().constructParametricType(List.class, ItemDeliverRegion.ProvinceRegion.class);
        /**
         * 构造provinceRegions对象,生成json字符串;用来对比前台传过来的json串格式是否正确
         */

        constructProvinceList();
        try {
            String tmpJsonStr = mapper.writeValueAsString(provinceRegionConstructs);
            List<ItemDeliverRegion.ProvinceRegion> provinceList = mapper.readValue(tmpJsonStr, pRegionListType);

            /**
             * 前台传过来的字符串,生成的json对象
             */
            provinceList = mapper.readValue(provinceJsonStr, pRegionListType);

            return provinceList;
        } catch (IOException var3) {
            return null;
        }
    }

    public static void constructProvinceList() {
        for(int i = 1; i < 3; i++) {
            ItemDeliverRegion.ProvinceRegion provinceRegion = new ItemDeliverRegion.ProvinceRegion();
            ItemDeliverRegion.Region province = new ItemDeliverRegion.Region();
            provinceRegion.setProvince(province);
            provinceRegion.getProvince().setId(i);
            provinceRegion.getProvince().setName("浙江省"+i);
            provinceRegion.getProvince().setSelectAll(true);
            provinceRegionConstructs.add(provinceRegion);
            List<ItemDeliverRegion.CityRegion> cities = new ArrayList<ItemDeliverRegion.CityRegion>();
            for(int j = 1; j < 3; j++) {
                ItemDeliverRegion.Region cRegion = new ItemDeliverRegion.Region();
                cRegion.setId(j);
                cRegion.setName("杭州市"+j);
                cRegion.setSelectAll(Boolean.TRUE);
                ItemDeliverRegion.CityRegion city = new ItemDeliverRegion.CityRegion();
                city.setCity(cRegion);
                cities.add(city);
            }
            provinceRegion.setCities(cities);
        }
    }
}
