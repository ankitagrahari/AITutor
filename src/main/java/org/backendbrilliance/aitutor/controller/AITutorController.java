package org.backendbrilliance.aitutor.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tutor")
public class AITutorController {

    ResponseEntity<String> ok(){
        return ResponseEntity.ok("up");
    }
}
