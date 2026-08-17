package app.medicamentos.auth;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.UUID;

@RequestScoped
public class CurrentUser {
    @Inject CurrentUserResolver resolver;
    private UserEntity user;
    private String candidateEmail;
    private String sub;
    private String candidateDisplayName;
    private boolean admin;

    public UUID id() { return resolved().id; }
    public String email() { return resolved().email; }
    public String displayName() { return resolved().displayName; }
    public boolean admin() { return admin; }

    void initialize(String email, String sub, String displayName, boolean admin) {
        this.candidateEmail = email;
        this.sub = sub;
        this.candidateDisplayName = displayName;
        this.admin = admin;
    }

    private UserEntity resolved() {
        if (user == null) user = resolver.resolve(candidateEmail, sub, candidateDisplayName, admin);
        return user;
    }
}
