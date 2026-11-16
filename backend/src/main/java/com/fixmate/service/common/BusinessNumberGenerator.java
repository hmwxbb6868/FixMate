package com.fixmate.service.common;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

/** 进程内唯一：UTC逻辑毫秒＋六位base36后缀；跨进程仍由数据库唯一键兜底。 */
public final class BusinessNumberGenerator {
    private static final long SUFFIX_SPACE = 2_176_782_336L; // 36^6
    // 与36互素，使同一毫秒内的后缀在完整周期内一一对应，不靠概率抽样去重。
    private static final long STEP = 1_000_003L;
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").withZone(ZoneOffset.UTC);
    private final Clock clock;
    private final LongSupplier randomSeed;
    private long logicalMillis = Long.MIN_VALUE;
    private long sequence;
    private long offset;

    public BusinessNumberGenerator() {
        this(Clock.systemUTC(), () -> ThreadLocalRandom.current().nextLong(SUFFIX_SPACE));
    }

    BusinessNumberGenerator(Clock clock, LongSupplier randomSeed) {
        this.clock = clock;
        this.randomSeed = randomSeed;
    }

    public synchronized String next(String prefix) {
        long wallMillis = clock.millis();
        if (wallMillis > logicalMillis || sequence == SUFFIX_SPACE) {
            logicalMillis = sequence == SUFFIX_SPACE ? Math.max(wallMillis, logicalMillis + 1) : wallMillis;
            sequence = 0;
            offset = Math.floorMod(randomSeed.getAsLong(), SUFFIX_SPACE);
        }
        long value = (offset + sequence++ * STEP) % SUFFIX_SPACE;
        String suffix = Long.toString(value, 36);
        return prefix + FORMAT.format(Instant.ofEpochMilli(logicalMillis)) + "0".repeat(6 - suffix.length()) + suffix;
    }
}
