package pbl6.daotao.units.config;
import java.time.Duration;
import java.util.UUID;

public record UnitExportProperties(UUID sourceEpoch,int maxRows,long maxJsonBytes,int maxActive,
        long maxRetainedBytes,Duration readTimeout,Duration ttl) {
    public UnitExportProperties {
        if(sourceEpoch==null) throw new IllegalArgumentException("Configured source epoch is required");
        if(maxRows<1 || maxRows>10000 || maxJsonBytes<1 || maxJsonBytes>8L*1024*1024
                || maxActive<1 || maxActive>2 || maxRetainedBytes<1 || maxRetainedBytes>16L*1024*1024
                || readTimeout==null || readTimeout.isNegative() || readTimeout.isZero()
                || readTimeout.compareTo(Duration.ofSeconds(30))>0
                || ttl==null || ttl.isNegative() || ttl.isZero() || ttl.compareTo(Duration.ofMinutes(15))>0) {
            throw new IllegalArgumentException("Export limits must stay within the approved units profile");
        }
    }
    public static UnitExportProperties defaults(UUID epoch) {
        return new UnitExportProperties(epoch,10000,8L*1024*1024,2,16L*1024*1024,
                Duration.ofSeconds(30),Duration.ofMinutes(15));
    }
}

