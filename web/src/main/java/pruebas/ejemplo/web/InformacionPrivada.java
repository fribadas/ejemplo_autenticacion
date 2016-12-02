package pruebas.ejemplo.web;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import pruebas.ejemplo.daos.UsuarioDAO;
import pruebas.ejemplo.entidades.Usuario;

@Named
@RequestScoped
public class InformacionPrivada {

    @EJB
    UsuarioDAO usuarioDAO;

    private Usuario usuarioAutenticado;

    @PostConstruct
    public void inicializar() {
        Principal principal = FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();
        if (principal != null) {
            usuarioAutenticado = usuarioDAO.buscarPorLogin(principal.getName());
        }
    }

    public Usuario getUsuarioAutenticado() {
        return usuarioAutenticado;
    }

    public List<Usuario> listadoUsuarios() {
        List<Usuario> usuarios = Collections.emptyList();
        try {
            usuarios = usuarioDAO.buscarTodos();
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Error invocando UsuarioDAO.buscarTodos()", e.getMessage()));
        }
        return usuarios;
    }
}
