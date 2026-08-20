package ua.stepan.zhuk.account;

import org.apache.ibatis.annotations.AutomapConstructor;
import ua.stepan.zhuk.account.balance.Balance;

import java.util.List;
import java.util.UUID;

public record Account(
        Long id,
        UUID publicId,
        UUID customerId,
        String country,
        Long createdAt,
        Long updatedAt,
        List<Balance> balances
) {
    @AutomapConstructor
    public Account(Long id, UUID publicId, UUID customerId, String country, Long createdAt, Long updatedAt) {
        this(id, publicId, customerId, country, createdAt, updatedAt, null);
    }

    public Account(UUID publicId, UUID customerId, String country, Long createdAt, Long updatedAt) {
        this(null, publicId, customerId, country, createdAt, updatedAt, null);
    }

    public static Account create(UUID publicId, UUID customerId, String country) {
        final long createdAt = System.currentTimeMillis();

        return new Account(publicId, customerId, country, createdAt, createdAt);
    }

    public static Account withIdAndBalances(Account account, Long id, List<Balance> balances) {
        return new Account(
                id,
                account.publicId(),
                account.customerId(),
                account.country(),
                account.createdAt(),
                account.updatedAt(),
                balances
        );
    }
}
