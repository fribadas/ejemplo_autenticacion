package pruebas.ejemplo.daos;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import pruebas.ejemplo.entidades.Usuario;

@Stateless
@DeclareRoles("registrado")
public class UsuarioDAO {

    @Resource
    protected SessionContext context;

    @PersistenceContext(unitName = "ejemplo_PU")
    private EntityManager em;

    public Usuario crear(Usuario usuario) {
        em.persist(usuario);
        return usuario; // Con ID asignado
    }

    public Usuario actualizar(Usuario usuario) {
        return em.merge(usuario);
    }

    public void borrar(Usuario usuario) {
        em.remove(em.merge(usuario));
    }

    //@PermitAll
    @RolesAllowed("registrado")
    public Usuario buscarPorLogin(String login) {
        System.out.println("[ejemplo] 'buscarPorLogin()' llamado por Principal.name = " 
                + context.getCallerPrincipal().getName() 
                + " (es 'registrado' = " + context.isCallerInRole("registrado") + ")");
        
        return em.find(Usuario.class, login);
    }

    //@PermitAll 
    @RolesAllowed("registrado")
    public List<Usuario> buscarTodos() {
        System.out.println("[ejemplo] 'buscarTodos()' llamado por Principal.name = " 
                + context.getCallerPrincipal().getName() 
                + " (es 'registrado' = " + context.isCallerInRole("registrado") + ")");
        
        TypedQuery<Usuario> q = em.createQuery("SELECT u FROM Usuario u", Usuario.class);
        return q.getResultList();
    }

    public void actualizarPassword(String login, String nuevoPassword) {
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA1");
            byte[] nuevoPasswordHash = hash.digest(nuevoPassword.getBytes());
            String nuevoPasswordHex = toHex(nuevoPasswordHash);

            Usuario usuario = buscarPorLogin(login);
            usuario.setPassword(nuevoPasswordHex);
            actualizar(usuario);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(UsuarioDAO.class.getName()).log(Level.SEVERE, "Fallo al crear hash", ex);
        }
    }

    /**
     * Funcion de utilidad para convertir byte[] en un cadena hexadecimal en
     * minuscula
     */
    private static String toHex(byte[] datos) {
        BigInteger bi = new BigInteger(1, datos);
        return String.format("%0" + (datos.length << 1) + "x", bi);
    }

}
