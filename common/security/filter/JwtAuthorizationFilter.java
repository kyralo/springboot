

import com.sharefarm.zzj.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

import static com.sharefarm.zzj.common.constants.SecurityConstants.*;


/**
 * \* Created with IntelliJ IDEA.
 * \* Description:jwt验证过滤器 进行token验证及用户授权
 * \
 */
public class JwtAuthorizationFilter extends BasicAuthenticationFilter {
    public JwtAuthorizationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String header = request.getHeader(HEADER_STRING);

        if (header == null || !header.startsWith(TOKEN_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }
        UsernamePasswordAuthenticationToken authenticationToken = getAuthentication(request);

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        chain.doFilter(request, response);

    }

    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
        String token = request.getHeader(HEADER_STRING);
        if (token != null) {
            // parse the token.
            Claims claims = JwtUtil.parseToken(token);
            if (claims != null) {
                //用户的唯一标识符
                String userIdentifier = claims.getAudience();
                String role = claims.getSubject();
                USERID = claims.get("userId",String.class);
                return new UsernamePasswordAuthenticationToken(userIdentifier, null,
                        // 设置角色
                        Collections.singletonList(new SimpleGrantedAuthority(role)));
            }
            return null;
        }
        return null;
    }
}
