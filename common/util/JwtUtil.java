

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;



/**
 * \* Created with IntelliJ IDEA.
 * \* Description:jwt工具类
 * \
 */
@Component
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    private static final Key KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    /**
     * 解析token
     * @param token the JWT token to parse
     * @return token claims
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(KEY)
                    .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
                    .getBody();
        } catch (JwtException e) {
            logger.warn("jwt验证失败" + e.getMessage());
            return null;
        }
    }

    /**
     *token授权
     */
    public static String generateTokenForAdmin(String username) {

        Claims claims = Jwts.claims()
                .setAudience(username).setSubject("ROLE_ADMIN");
        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(KEY)
                .compact();
    }

    /**
     * token
     * */
    public static String generateTokenById(String id,String userId) {
        Claims claims = Jwts.claims()
                .setAudience(id)
                .setSubject("ROLE_USER");

        //        新增值自定义字段
        Map<String,Object> map = new HashMap<>(1);
        map.put("userId",userId);

        return Jwts.builder()
                .setClaims(claims)
                .addClaims(map)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(KEY)
                .compact();
    }
}
