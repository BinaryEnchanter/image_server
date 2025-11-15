package image.server.image_server.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import image.server.image_server.model.RechargeTxn;
import image.server.image_server.model.User;
import image.server.image_server.repository.UserRepository;
import image.server.image_server.security.JwtUtil;
import image.server.image_server.service.RechargeService;
import io.jsonwebtoken.JwtException;

@RestController
@RequestMapping("/api/v1/users")
public class RechargeController {

    @Autowired
    private RechargeService rechargeService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private image.server.image_server.service.ActionLogService actionLogService;

    /**
     * POST /api/v1/users/me/recharge
     * Body: { "amount": 100, "note": "test", "provider_txn_id": "sim-123" }
     * Requires Authorization: Bearer <jwt>
     */
    @PostMapping("/me/recharge")
    public ResponseEntity<?> rechargeMe(@RequestHeader("Authorization") String authHeader,
                                        @RequestBody Map<String, Object> body) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body(Map.of("error", "unauthenticated"));
            }
            String token = authHeader.substring(7);
            String subj = jwtUtil.validateAndGetSubject(token);
            UUID userUuid = UUID.fromString(subj);

            // parse amount
            Object amtObj = body.get("amount");
            if (amtObj == null) return ResponseEntity.badRequest().body(Map.of("error", "amount required"));
            long amount;
            try {
                amount = Long.parseLong(amtObj.toString());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "invalid amount"));
            }
            if (amount <= 0) return ResponseEntity.badRequest().body(Map.of("error", "amount must > 0"));

            String note = (String) body.getOrDefault("note", "test recharge");
            String providerTxnId = (String) body.getOrDefault("provider_txn_id", null);

            RechargeTxn txn = rechargeService.rechargeUser(userUuid, amount, providerTxnId, note);

            // get updated user for response
            User u = userRepository.findById(userUuid).orElseThrow();

            actionLogService.log(userUuid, "recharge", null, "{\"txn_id\":" + txn.getId() + ",\"amount\":" + amount + "}");
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "txn_id", txn.getId(),
                    "new_coins", u.getCoins()
            ));
        } catch (JwtException jex) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid token"));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
        }
    }

    // Admin endpoint: list recharges (optional)
    @GetMapping("/recharges")
    public ResponseEntity<?> listRecharges(@RequestHeader("Authorization") String authHeader) {
        // Implement admin check if needed
        // ...
        return ResponseEntity.ok().build();
    }
}
