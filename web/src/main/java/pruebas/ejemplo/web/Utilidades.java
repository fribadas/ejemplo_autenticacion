package ejemplo.web;

import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Named
@RequestScoped
public class Utilidades {

    public String extraerNombrePrincipal() {
        Principal principal = FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
        if (principal != null) {
            return principal.getName();
        } else {
            return "<Principal no establecido>";
        }
    }

    public String doLogout() {
        FacesContext facesContet = FacesContext.getCurrentInstance();
        
        HttpServletRequest request = (HttpServletRequest) facesContet.getExternalContext().getRequest();
        try {
            request.logout();
        } catch (ServletException ex) {
            Logger.getLogger(Utilidades.class.getName()).log(Level.SEVERE, "Error haciendo logout", ex);
        }

        HttpSession session = (HttpSession) facesContet.getExternalContext().getSession(true);
        session.invalidate();
        return "/index?faces-redirect=true";
    }
}
