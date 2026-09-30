package com.example.demo.dto.request.User;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.tomcat.util.descriptor.web.SecurityRoleRef;

import java.time.LocalDate;
import java.util.Date;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRequest {
    @Column(nullable = false)
    private String fullname;
    @Column(nullable = false)
    private LocalDate doB;
    @Column(nullable = false)
    private  String phone;

}
