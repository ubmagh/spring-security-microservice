package ma.enset.productsapp.web;

import ma.enset.productsapp.entities.Supplier;
import ma.enset.productsapp.repositories.ProductRepository;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.springsecurity.client.KeycloakRestTemplate;
import org.keycloak.adapters.springsecurity.facade.SimpleHttpFacade;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ProductController{

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private KeycloakRestTemplate keycloakRestTemplate;

    @Autowired
    private AdapterDeploymentContext adapterDeploymentContext;

    @GetMapping("/")
    public String index(){
        return "index";
    }
    @GetMapping("/products")
    public String products(Model model){
        model.addAttribute("products",productRepository.findAll());
        return "products";
    }

    @GetMapping("/suppliers")
    public String suppliers( HttpServletRequest request, Model model){
        /*
        KeycloakAuthenticationToken token = (KeycloakAuthenticationToken) request.getUserPrincipal();
        KeycloakPrincipal principal = (KeycloakPrincipal) token.getPrincipal();
        KeycloakSecurityContext session = principal.getKeycloakSecurityContext();
        RestTemplate template = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer "+session.getTokenString());
        HttpEntity entity = new HttpEntity(headers);
        ResponseEntity<PagedModel<Supplier>> response = template.execute("http://localhost:8083/suppliers", HttpMethod.GET, entity, new Para)
        */

        PagedModel<Supplier> pagedSupp = keycloakRestTemplate.getForObject("http://localhost:8083/suppliers", PagedModel.class);// token header auto added
        model.addAttribute("suppliers", pagedSupp);

        return "suppliers";
    }

    @ExceptionHandler(Exception.class)
    public String exceptionHandler(Exception e, Model model){
        model.addAttribute("message",e.getMessage());
        return "errors";
    }


    @GetMapping("/jwt")
    @ResponseBody
    public Map<String,String> map(HttpServletRequest request) {
        KeycloakAuthenticationToken token = (KeycloakAuthenticationToken) request.getUserPrincipal();
        KeycloakPrincipal principal = (KeycloakPrincipal) token.getPrincipal();
        KeycloakSecurityContext session = principal.getKeycloakSecurityContext();
        Map<String,String> map = new HashMap<>();
        map.put("access_token", session.getTokenString());
        return map;
    }

    @GetMapping( "/profile" )
    public String changePassword(RedirectAttributes attributes, HttpServletRequest request, HttpServletResponse response) {
        HttpFacade facade = new SimpleHttpFacade(request, response);
        KeycloakDeployment deployment = adapterDeploymentContext.resolveDeployment(facade);
        attributes.addAttribute("referrer", deployment.getResourceName());
        attributes.addAttribute("referrer_uri", request.getHeader("referer"));
        return "redirect:" + deployment.getAccountUrl();
    }



}