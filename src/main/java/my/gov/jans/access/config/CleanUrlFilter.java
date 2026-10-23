package my.gov.jans.access.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Hides the ".html" extension: "/petugas.html" is redirected to "petugas"
 * (relative, so any reverse-proxy prefix is preserved) and "/petugas" is
 * forwarded internally to the static "/petugas.html".
 */
@Component
// Must run after Spring Security (order -100): forwards are not secured again\n@Order(0)
public class CleanUrlFilter extends OncePerRequestFilter {

    private static final Set<String> PAGES = Set.of("admin", "aduan", "faq", "forgot-password",
            "forgot-password-verify", "hubungi", "login", "mohon", "pelawat", "pengarah", "penyelia-loji",
            "petugas", "profile", "sah", "semak");

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (!"GET".equals(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }
        String path = req.getRequestURI().substring(req.getContextPath().length());
        int slash = path.lastIndexOf('/');
        String name = path.substring(slash + 1);

        if (slash == 0 && (name.equals("index.html") || name.equals("index"))) {
            String qs = req.getQueryString();
            res.setStatus(HttpServletResponse.SC_FOUND);
            res.setHeader("Location", qs == null ? "./" : "./?" + qs);
            return;
        }
        if (name.endsWith(".html") && slash == 0 && PAGES.contains(name.substring(0, name.length() - 5))) {
            String clean = name.substring(0, name.length() - 5);
            String qs = req.getQueryString();
            res.setStatus(HttpServletResponse.SC_FOUND);
            res.setHeader("Location", qs == null ? clean : clean + "?" + qs);
            return;
        }
        if (slash == 0 && PAGES.contains(name) && req.getDispatcherType() == jakarta.servlet.DispatcherType.REQUEST) {
            req.getRequestDispatcher("/" + name + ".html").forward(req, res);
            return;
        }
        chain.doFilter(req, res);
    }
}
