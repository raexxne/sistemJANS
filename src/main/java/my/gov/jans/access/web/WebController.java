package my.gov.jans.access.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
    @GetMapping("/utama")
    String utama(Authentication a) {
        if (a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin.html";
        }
        if (a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_PENGARAH"))) {
            return "redirect:/pengarah.html";
        }
        if (a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_PENYELIA_LOJI"))) {
            return "redirect:/penyelia-loji.html";
        }
        if (a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_CGSO"))) {
            return "redirect:/cgso.html";
        }
        if (a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_KPP_CAWANGAN_PENGELUARAN"))) {
            return "redirect:/kpp-cawangan-pengeluar.html";
        }
        return "redirect:/petugas.html";
    }
}
