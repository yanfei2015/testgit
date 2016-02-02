package com.yanfei;

import java.io.Serializable;
import java.util.List;

/**
 * Created by dev001 on 16/2/2.
 */

public class ItemDeliverRegion implements Serializable {
    private static final long serialVersionUID = -3887521430745320145L;

    public static class Region implements Serializable {
        private static final long serialVersionUID = 6833277918100033083L;
        private Integer id;
        private String name;
        Boolean selectAll;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Boolean getSelectAll() {
            return selectAll;
        }

        public void setSelectAll(Boolean selectAll) {
            this.selectAll = selectAll;
        }
    }

    public static class CityRegion implements Serializable{
        private static final long serialVersionUID = -6364688789258700591L;
        private Region city;
        private List<Region> regions;

        public static long getSerialVersionUID() {
            return serialVersionUID;
        }

        public Region getCity() {
            return city;
        }

        public void setCity(Region city) {
            this.city = city;
        }

        public List<Region> getRegions() {
            return regions;
        }

        public void setRegions(List<Region> regions) {
            this.regions = regions;
        }
    }

    public static class ProvinceRegion implements Serializable{
        private static final long serialVersionUID = 254221740883332387L;
        private Region province;
        private List<CityRegion> cities;

        public static long getSerialVersionUID() {
            return serialVersionUID;
        }

        public Region getProvince() {
            return province;
        }

        public void setProvince(Region province) {
            this.province = province;
        }

        public List<CityRegion> getCities() {
            return cities;
        }

        public void setCities(List<CityRegion> cities) {
            this.cities = cities;
        }
    }
}
