package image.server.image_server.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import image.server.image_server.model.RechargeTxn;
import image.server.image_server.model.User;
import image.server.image_server.repository.RechargeRepository;
import image.server.image_server.repository.UserRepository;

/**
 * 充值业务（测试模式）
 */
@Service
public class RechargeService {

    @Autowired
    private RechargeRepository rechargeRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 给用户充值（加 coins），在事务内完成：写流水 -> 更新用户 coins
     * 返回充值流水实体
     */
    @Transactional
    public RechargeTxn rechargeUser(UUID userUuid, long amountCoins, String providerTxnId, String note) {
        if (amountCoins <= 0) throw new IllegalArgumentException("amount must be > 0");

        // 1. create txn record
        RechargeTxn txn = new RechargeTxn();
        txn.setUserUuid(userUuid);
        txn.setAmountCoins(amountCoins);
        txn.setAmountCents(0L);
        txn.setProviderTxnId(providerTxnId);
        txn.setStatus("completed"); // 测试阶段直接完成
        txn.setNote(note);
        rechargeRepository.save(txn);

        // 2. update user coins (atomic within transaction)
        User user = userRepository.findById(userUuid).orElseThrow(() -> new RuntimeException("user not found"));
        long newCoins = (user.getCoins() == null ? 0L : user.getCoins()) + amountCoins;
        user.setCoins(newCoins);
        userRepository.save(user);

        return txn;
    }
}
