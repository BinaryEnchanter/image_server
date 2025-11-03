package image.server.image_server.repository;


import image.server.image_server.model.RechargeTxn;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RechargeRepository extends JpaRepository<RechargeTxn, Long> {
    // 可按需增加查询方法
}
