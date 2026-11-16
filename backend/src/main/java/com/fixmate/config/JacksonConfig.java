package com.fixmate.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /**
     * 冻结规则 11.1：业务主键（18–19 位）必须转字符串避免 JS 精度丢失。
     * 只转换 ID 类字段，计数、字节数、分页 total 等保持 number。
     */
    @Bean
    public Module fixmateSerializationModule() {
        SimpleModule module = new SimpleModule();
        // 只转包装类型 Long（ID），保留 long 基本类型（计数、字节数、total）为数字
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(BigDecimal.class, ToStringSerializer.instance);
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
        return module;
    }

    /** 数据库存 UTC，API 返回带偏移的 ISO-8601（契约要求）。 */
    static class LocalDateTimeSerializer extends StdSerializer<LocalDateTime> {
        LocalDateTimeSerializer() {
            super(LocalDateTime.class);
        }

        @Override
        public void serialize(LocalDateTime value, com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider)
                throws java.io.IOException {
            gen.writeString(OffsetDateTime.of(value, java.time.ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
    }
}
