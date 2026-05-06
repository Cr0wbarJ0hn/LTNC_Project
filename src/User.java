import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public abstract class User implements Serializable{
    private static final long serialVersionUID = 1L;

    private String id;
    private String userName;
    private String email;
    private String passWordHash;
    private String role; // "Admin, Bidder, Seller"
    private boolean isActive;

}