

package me.ubmagh.securityservicelegacy.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RestApiTest {

    @GetMapping("/testGetData")
    @PreAuthorize("hasAuthority('SCOPE_USER')")
    public Map<String, Object> dataTest(Authentication authentication){
        return Map.of(
                "message","This is testing data",
                "username",authentication.getName(),
                "authorities",authentication.getAuthorities()
        );
    }

    @PostMapping("/testSaveData")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    public Map<String,String> saveData(String data){
        return Map.of("saved_data",data);
    }

}
