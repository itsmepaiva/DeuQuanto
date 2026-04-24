package portifolio.deuquanto.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import portifolio.deuquanto.dto.UserProfileDTO;
import portifolio.deuquanto.dto.request.UserPatchRequest;
import portifolio.deuquanto.entity.Users;
import portifolio.deuquanto.repository.GroupMemberRepository;
import portifolio.deuquanto.repository.UserRepository;
import portifolio.deuquanto.service.UserService;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UsersServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private Users mockUser;

    @BeforeEach
    void setUp() {
        // Antes de CADA teste, preparamos o terreno com dados limpos
        userId = UUID.randomUUID();

        mockUser = new Users();
        mockUser.setId(userId);
        mockUser.setName("Test");
        mockUser.setEmail("test@email.com");
        mockUser.setPixKey("11122233344");
    }

    @Test
    @DisplayName("Deve retornar o perfil do usuário corretamente")
    void shouldReturnUserProfile() {
        // ARRANGE: Quando o service chamar o banco, devolva o nosso mockUser
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // ACT: Executamos o método
        UserProfileDTO result = userService.getUserProfile(userId);

        // ASSERT: Verificamos se o DTO mapeou os dados corretamente
        assertNotNull(result);
        assertEquals("Test", result.name());
        assertEquals("test@email.com", result.email());
        assertEquals("11122233344", result.pixKey());
    }

    @Test
    @DisplayName("Deve atualizar apenas o nome e manter a chave PIX (Regra do PATCH)")
    void shouldUpdateOnlyProvidedFieldsOnPatch() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        UserPatchRequest request = new UserPatchRequest("Novo Nome Sênior", null);

        userService.updateUserProfile(userId, request);

        // ASSERT: O nome deve mudar, mas o Pix deve continuar intacto!
        assertEquals("Novo Nome Sênior", mockUser.getName());
        assertEquals("11122233344", mockUser.getPixKey()); // A prova de que o PATCH funciona

        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("NÃO deve permitir excluir usuário se ele estiver em grupos ativos")
    void shouldThrowExceptionWhenDeletingUserInActiveGroup() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        when(groupMemberRepository.existsByUserId(userId)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.deleteUserAccount(userId);
        });

        assertTrue(exception.getMessage().contains("grupos ativos"));

        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Deve deletar a conta se o usuário não tiver vínculos com grupos")
    void shouldAllowDeleteWhenUserHasNoGroups() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(groupMemberRepository.existsByUserId(userId)).thenReturn(false);

        userService.deleteUserAccount(userId);

        verify(userRepository, times(1)).delete(mockUser);
    }
}
