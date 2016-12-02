## Descripción
Ejemplo de autenticación y control de acceso en capa web y en EJBs usando WildFly.

Se hace uso de los mecanismos de autenticación ofrecidos por el servidor de aplicaciones (la otra alternativa es el control "manual" o empleando librerías como Spring Security).

En este caso se utiliza la autentiación basada en base de datos (MySQL en este caso) y en el mecanismo de control de acceso de EJB (`@RolesAllowed`)

## Configuración previa
1. Crear la BD, un usuario `ejemplo` y cargar los datos iniciales

Desde el cliente `mysql`con el comando `mysql -u root -p`

```
CREATE DATABASE ejemplo_autenticacion;
GRANT ALL PRIVILEGES ON ejemplo_autenticacion.* TO ejemplo@localhost IDENTIFIED BY "ejemplo";


USE ejemplo_autenticacion;

DROP TABLE IF EXISTS usuario;
CREATE TABLE usuario (
  login varchar(255) DEFAULT NULL,
  nombre varchar(255) DEFAULT NULL,
  password varchar(255) DEFAULT NULL,
  PRIMARY KEY (login)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO usuario(login,nombre, password)
        VALUES ('ana','ana ana ana',SHA1('ana')),
               ('eva','eva eva eva',SHA1('eva')),
               ('fran','fran fran fran',SHA1('fran'));
```



## Configuración de WildFly 8.2.21
Configuración exclusiva de WildFly para definir la autenticación basada en BD y vincularla a los proyecto _web_ y _ejb_.

  * Disponible en el fichero `standalone-ejemplo.xml` incluido en el directorio `configuracion_previa`

1. Definir el _Datasource_ de nombre `ejemplo_autenticacion` a utilizar tanto por la propia aplicación como por el mecanismo de autenticación basado en BD.
```xml
...
<!-- Declaración DATASOURCE  -->
<datasource jta="true" jndi-name="java:jboss/datasources/ejemplo_autenticacion"
            pool-name="ejemplo_autenticacion" enabled="true" use-ccm="true">
    <connection-url>jdbc:mysql://localhost:3306/ejemplo_autenticacion</connection-url>
    <driver-class>com.mysql.jdbc.Driver</driver-class>
    <driver>mysql-connector-java-5.1.21.jar</driver>
   <security>
       <user-name>ejemplo</user-name>
       <password>ejemplo</password>
   </security>
</datasource>
...
<!-- Declaración DATASOURCE  -->
```
2. Definir el _Security Domain_ de nombre `ejemplo-autenticacion-security-domain`, vinculado al _Datasource_ anterior, con la configuración a utilizar en el control de acceso
```xml
...
<!-- Declaracion SECURITY DOMAIN -->
<security-domain name="ejemplo-autenticacion-security-domain">
    <authentication>
        <login-module code="Database" flag="required">
            <module-option name="dsJndiName" value="java:jboss/datasources/ejemplo_autenticacion" />
            <module-option name="principalsQuery"
                                           value="SELECT password FROM usuario WHERE login=?" />
            <module-option name="rolesQuery"
                                           value="SELECT 'registrado', 'Roles' FROM usuario WHERE login=?" />  
					  <!-- fuerza que todos los usuarios autenticados tengal el rol 'registrado'-->
            <module-option name="hashAlgorithm" value="SHA1"/>
					  <!-- BD almacena hash SHA1 de password real -->
            <module-option name="hashEncoding" value="hex"/>
					  <!-- BD codifica el hash en caracteres hexadecimal en minúsculas [opción por defecto en SHA1() de MySQL] -->
            <module-option name="ignorePasswordCase" value="true"/>
       </login-module>
    </authentication>
</security-domain>
...
<!-- Declaracion SECURITY DOMAIN -->
```
  Opciones relevantes
  * `dsJndiName`: nombre JNDI del _Datasource_ que gestiona el acceso a la BD
  * `principalsQuery`: consulta a lanzar para extraer de la BD la contraseña vinculada al nombre del _Principal_ que pretende acceder.
  * `rolesQuery`: consulta a lanzar para extraer de la BD el rol (tipo de usuario) vinculado al nombre del _Principal_ que pretende acceder (en este caso devuelve siempre `registrado`)
  * `hashAlgorithm`: algoritmo de HASH criptográfico usado para el almacenamiento de las contraseñas (en este caso SHA1)
  * `hashEncoding`: esquema de codificación del los HASHES (en este caso el HASH binario SHA1 [de 160 bits] se codifica en hexadecimal, usando minúsculas, resultando en 40 caraceres imprimibles [0-9a-f])

3. En el subproyecto _web_, incluir en `WEB_INF` el fichero `jboss-web.xml`, donde se vincula a la aplicación web con el _Securiy Domain_ `ejemplo-autenticacion-security-domain`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jboss-web>
    <security-domain>ejemplo-autenticacion-security-domain</security-domain>
</jboss-web>
```
4. En el subproyecto ejb, incluir en `META_INF` el fichero `jboss-ejb3.xml`, donde se vincula el subproyecto con el _Securiy Domain_ `ejemplo-autenticacion-security-domain`
```xml
<?xml version="1.1" encoding="UTF-8"?>
<jboss:ejb-jar xmlns:jboss="http://www.jboss.com/xml/ns/javaee"
               xmlns="http://java.sun.com/xml/ns/javaee"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xmlns:s="urn:security:1.1"
               xsi:schemaLocation="http://www.jboss.com/xml/ns/javaee http://www.jboss.org/j2ee/schema/jboss-ejb3-2_0.xsd http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_1.xsd"
               version="3.1"
               impl-version="2.0">
    <assembly-descriptor>
        <s:security>
            <ejb-name>*</ejb-name>
            <s:security-domain>ejemplo-autenticacion-security-domain</s:security-domain>
        </s:security>
    </assembly-descriptor>
</jboss:ejb-jar>
```


## Configuración del control de acceso en el subproyecto _web_
Esta configuración está recogida en las especificiaciones de Java EE y es común a otros servidores de aplicaciones.

Añadir en el fichero `WEB_INF/web.xml` la definición del método de login basado en formulario (_Form-BAses Login_), los roles de seguridad definidos para los distintos tipos de usuarios y vincular las URLs de la aplicación a cada uno de estos roles.
```xml
...
<login-config>
        <auth-method>FORM</auth-method>
        <realm-name>ejemploRealm</realm-name>
        <form-login-config>
            <form-login-page>/faces/login.xhtml</form-login-page>
            <form-error-page>/faces/login-error.xhtml</form-error-page>
        </form-login-config>
</login-config>

<security-role>  
        <role-name>registrado</role-name>  
</security-role>  

<security-constraint>
        <display-name>Area privada para usuarios autenticados</display-name>
        <web-resource-collection>
            <web-resource-name>Area privada</web-resource-name>
            <description></description>
            <url-pattern>/faces/privado/*</url-pattern>
            <http-method>GET</http-method>
            <http-method>POST</http-method>
        </web-resource-collection>
        <auth-constraint>
            <description/>
            <role-name>registrado</role-name>
        </auth-constraint>
        <user-data-constraint>
            <transport-guarantee>NONE</transport-guarantee>
        </user-data-constraint>
</security-constraint>
...
```
   * En este caso la `<security-constraint>` limita el acceso a las URL dentro del patrón `/faces/privado/*` a usuarios con el rol `registrado`.  
   * En el caso de prtender acceder a esas URLs sin estar autenticado, se redirige la navegación a la página de login indicada en `<form-login-page>`
   * Si el login tiene éxito se continúa la navegación al destino indicado y, si falla, se redirige la navegación a la página de error declarada en `<form-error-page>`.


### Página de login (subproyecto _web_)
Disponible en `web/src/main/webapp/login.xhtml`
```xml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:h="http://xmlns.jcp.org/jsf/html">
    <h:head>
        <title>Página de Login</title>
    </h:head>
    <h:body>
        <h1> Página de Login </h1>

        <h:form id="loginForm" onsubmit="document.loginForm.action = 'j_security_check';">  
            <h:panelGrid columns="2">
                <h:outputLabel value="Login"></h:outputLabel>  
                <input name="j_username" type="text" />  

                <h:outputLabel value="Contraseña"></h:outputLabel>  
                <input name="j_password" size="15" type="password" />  

                <h:commandButton value="Acceder"></h:commandButton>  
            </h:panelGrid>
        </h:form>  

    </h:body>
</html>
```
   Declara un formulario JSF convencional, pero el `<h:commandButton>` no está vinculado con ningún método de accion de los _Managed Beans_. En su lugar, la acción de la petición POST  está etiquetada con  `j_security_check`
   y los campos de entrada de _Login_ y _Contraseña_ tienen obligatoriamente los nombres `"j_username"` y `"j_password"`.

## Detalles de configuación en el proyecto _ejb_
### Declaración de la unidad de persistencia (_PersistenceUnit_)
Disponible en `ejb/src/main/resources/META-INF/persistence.xml`, se vincula con el _Datasource_ declarado en el fichero de configuración `standalone-ejemplo.xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<persistence version="2.1" xmlns="http://xmlns.jcp.org/xml/ns/persistence"
                           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                           xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence
                                               http://xmlns.jcp.org/xml/ns/persistence/persistence_2_1.xsd">
  <persistence-unit name="ejemplo_PU" transaction-type="JTA">
    <jta-data-source>java:/jboss/datasources/ejemplo_autenticacion</jta-data-source>
    <exclude-unlisted-classes>false</exclude-unlisted-classes>
    <properties>
    </properties>
  </persistence-unit>
</persistence>
```

### Declaración de la entidad `Usuario`
Disponible en `ejb/src/main/java/ejemplo/entidades/Usuario.java`.

* La entidad tiene una anotación `@Table(name="usuario")` para garantizar que la tabla MySQL utilizada coincide con la que se emplea en la consulta definida en el parámetro `principalsQuery` del _Security Domain_ `ejemplo-autenticacion-security-domain` del fichero de configuración `standalone-ejemplo.xml`.
* Las propiedades `login`, `password` están anotadas con `@Column(name="..."")` para garantizar que los nombre de as columnas de la tabla `usuario` coincida con las indicadas en la consulta definida en el parámetro `principalsQuery` del _Security Domain_ `ejemplo-autenticacion-security-domain` del fichero de configuración `standalone-ejemplo.xml`.

### EJB `UsuarioDAO`
Disponible en `ejb/src/main/java/ejemplo/daos/UsuarioDAO.java`.

* Incluye a nivel de clase la anotación `@DeclareRoles("registrado")`, indicando el tipo de roles que potencialmente accederán al EJB
* Los métodos `buscarTodos()` y `buscarPorLogin()` están anotados con `@RolesAllowed("registrado")` (realmente es redudante). Se puede probar a reemplazar la anotación por `@PermitAll` para ver el cambio en el comportamiento de la apliciación web.
* Se incluye como ejemplo un método `actualizarPassword()` para mostar cómo generar desde código contraseñas válidas (se apoya en la función auxiliar `toHex()` para generar la codificación como cadena Hexadecimal)

## Puesta en marcha

1. Clonar y compilar el proyecto

```
~/:$  git clone https://github.com/fribadas/ejemplo_autenticacion.git
~/:$  cd ejemplo_autenticacion
ejemplo_autenticacion/:$  mvn install
```


2. Arrancar WildFly con la configuración descrita 

Puede usarse el `standalone-ejemplo.xml` aportado [asume que se cuenta con `mysql-connector-java-5.1.21.jar`] o adaptar una configuración ya existente.

($WILDFLY_HOME se corresponde con el directorio de instalación de WildFly 8.2.1)

```
ejemplo_autenticacion/:$  cp configuracion/standalone-ejemplo.xml $WILDFLY_HOME/standalone/configure
ejemplo_autenticacion/:$  $WILDFLY_HOME/bin/standalone -c $WILDFLY_HOME/standalone/configure
```


3. Desplegar manualmente el proyecto _web_

Copiar war del subproyecto _web_ en el directorio `deployments`de WildFly.

```
ejemplo_autenticacion/:$  cp web/target/web-1.0-SNAPSHOT.war $WILDFLY_HOME/standalone/deployments
```

El ejemplo estará accesible en la URL (http://localhost:8080/web-1.0-SNAPSHOT/).
