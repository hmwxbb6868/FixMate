package com.fixmate.security;

import com.fixmate.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class StaffJwtService {
    private final SecretKey key; private final JwtProperties properties;
    public StaffJwtService(JwtProperties properties) {this.properties=properties;key=Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));}
    public String issue(Long id,String family,Integer version,String use) {
        Instant now=Instant.now();long ttl="ACCESS".equals(use)?properties.getAccessTtlSeconds():properties.getRefreshTtlSeconds();
        return Jwts.builder().issuer("fixmate").subject(id.toString()).audience().single("fixmate-admin")
            .id(UUID.randomUUID().toString()).claim("principalType","STAFF").claim("tokenUse",use)
            .claim("familyId",family).claim("credentialVersion",version).issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(ttl))).signWith(key,Jwts.SIG.HS256).compact();
    }
    public StaffPrincipal parse(String token,String use) {
        try {
            Jws<Claims> signed=Jwts.parser().verifyWith(key).requireIssuer("fixmate")
                .require("principalType","STAFF").require("tokenUse",use).build().parseSignedClaims(token);
            Claims c=signed.getPayload();
            if(!"HS256".equals(signed.getHeader().getAlgorithm())||c.getAudience()==null||c.getAudience().size()!=1
                    ||!c.getAudience().contains("fixmate-admin")||c.getId()==null||c.getId().length()<16
                    ||c.get("familyId",String.class)==null||c.get("familyId",String.class).length()<16
                    ||c.get("credentialVersion",Integer.class)==null||c.get("credentialVersion",Integer.class)<0
                    ||c.getIssuedAt()==null||c.getExpiration()==null)throw new JwtException("domain");
            Long id=Long.valueOf(c.getSubject());if(id<=0)throw new JwtException("subject");
            return new StaffPrincipal(id,c.get("familyId",String.class),c.getId(),c.get("credentialVersion",Integer.class));
        } catch(RuntimeException ex) {throw new JwtException("invalid staff token",ex);}
    }
}
