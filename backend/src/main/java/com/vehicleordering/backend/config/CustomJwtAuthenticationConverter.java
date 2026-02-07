package com.vehicleordering.backend.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String GROUPS_CLAIM = "groups";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // Extract groups from Okta JWT token
        Object groupsClaim = jwt.getClaims().get(GROUPS_CLAIM);

        if (groupsClaim instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<String> groups = (Collection<String>) groupsClaim;
            return groups.stream()
                    .map(group -> new SimpleGrantedAuthority(ROLE_PREFIX + group.toUpperCase()))
                    .collect(Collectors.toList());
        }

        // Default role if no groups found
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
