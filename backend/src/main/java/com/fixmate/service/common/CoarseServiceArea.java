package com.fixmate.service.common;

import com.fasterxml.jackson.databind.ObjectMapper;

/** 只读独立行政区域字段；不拼接province/contact/detail，也不截取原始地址。 */
public final class CoarseServiceArea {
    private CoarseServiceArea() {}
    public static String fromSnapshot(ObjectMapper json,String snapshot) {
        if(snapshot==null)return null;
        try {
            var data=json.readTree(snapshot);
            if(data==null||!data.isObject())return null;
            var city=data.get("city");var district=data.get("district");
            if(city==null||district==null||!city.isTextual()||!district.isTextual())return null;
            String c=city.asText(),d=district.asText();
            if(c.length()>32||d.length()>32||!c.matches("[\\p{IsHan}·]+(市|自治州|地区|盟)")
                    ||!d.matches("[\\p{IsHan}·]+(区|县|市|旗)"))return null;
            if((c+d).matches(".*(路|街|巷|弄|号|栋|室|门牌).*"))return null;
            return c+d;
        }catch(com.fasterxml.jackson.core.JsonProcessingException|IllegalArgumentException ex){return null;}
    }
}
