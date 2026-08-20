package ua.stepan.zhuk.infrastructure.typehandler;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UuidTypeHandlerTest {

    private final UuidTypeHandler handler = new UuidTypeHandler();

    @Test
    void givenUuidParameter_whenSetNonNullParameter_thenDelegatesUuidAsObject() throws Exception {
        PreparedStatement statement = mock(PreparedStatement.class);
        UUID uuid = UUID.randomUUID();

        handler.setNonNullParameter(statement, 2, uuid, JdbcType.OTHER);

        verify(statement).setObject(2, uuid);
    }

    @Test
    void givenColumnName_whenGetNullableResult_thenReadsUuid() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        UUID uuid = UUID.randomUUID();
        when(resultSet.getObject("public_id", UUID.class)).thenReturn(uuid);

        assertThat(handler.getNullableResult(resultSet, "public_id")).isEqualTo(uuid);
    }

    @Test
    void givenColumnIndex_whenGetNullableResult_thenReadsUuid() throws Exception {
        ResultSet resultSet = mock(ResultSet.class);
        UUID uuid = UUID.randomUUID();
        when(resultSet.getObject(1, UUID.class)).thenReturn(uuid);

        assertThat(handler.getNullableResult(resultSet, 1)).isEqualTo(uuid);
    }

    @Test
    void givenCallableStatementColumnIndex_whenGetNullableResult_thenReadsUuid() throws Exception {
        CallableStatement statement = mock(CallableStatement.class);
        UUID uuid = UUID.randomUUID();
        when(statement.getObject(3, UUID.class)).thenReturn(uuid);

        assertThat(handler.getNullableResult(statement, 3)).isEqualTo(uuid);
    }
}
