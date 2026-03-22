package com.mihai.overview.service;

import com.mihai.overview.dto.request.AuthenticationRequest;
import com.mihai.overview.dto.request.RegisterRequest;
import com.mihai.overview.dto.response.AuthenticationResponse;

public interface AuthenticationService {
    void register(RegisterRequest input) throws Exception;
    AuthenticationResponse login (AuthenticationRequest request);
}
