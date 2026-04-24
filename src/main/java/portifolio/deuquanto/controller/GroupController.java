package portifolio.deuquanto.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import portifolio.deuquanto.dto.JWTUserData;
import portifolio.deuquanto.dto.request.AddMemberRequest;
import portifolio.deuquanto.dto.request.CreateGroupRequest;
import portifolio.deuquanto.dto.response.GenericGroupResponse;
import portifolio.deuquanto.dto.response.CreateGroupResponse;
import portifolio.deuquanto.dto.response.GroupDetailedResponse;
import portifolio.deuquanto.dto.response.GroupSummaryResponse;
import portifolio.deuquanto.entity.Group;
import portifolio.deuquanto.entity.GroupMember;
import portifolio.deuquanto.entity.enums.GroupRole;
import portifolio.deuquanto.service.GroupService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/group")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/create")
    @Operation(
            summary = "Cria novo grupo")
    public ResponseEntity<CreateGroupResponse> createGroup(@AuthenticationPrincipal JWTUserData loggedUser,
                                                           @Valid @RequestBody CreateGroupRequest request){

        Group createdGroup = groupService.createGroup(loggedUser.userId(), request);

        CreateGroupResponse response = CreateGroupResponse.from(createdGroup, GroupRole.ADMIN);

        return ResponseEntity.status(HttpStatusCode.valueOf(201))
                .body(response);
    }

    @GetMapping("/member")
    @Operation(
            summary = "Retorna lista de membros grupo")
    public ResponseEntity<List<GenericGroupResponse>> getMembersGroup(@AuthenticationPrincipal JWTUserData loggedUser){
        List<GroupMember> membership = groupService.getAllMembersGroups(loggedUser.userId());

        List<GenericGroupResponse> responseList = membership.stream()
                .map(gm -> GenericGroupResponse.from(gm.getGroup(), gm.getRole()))
                .toList();

        return ResponseEntity.ok(responseList);
    }


    @PreAuthorize("@groupSecurity.isGroupAdmin(#loggedUser.userId, #groupId)")
    @PostMapping("/{groupId}/member/new")
    @Operation(
            summary = "Adiciona membro",
            description = "Adiciona membro manualmente no grupo, apenas o admin pode realizar essa função."
    )
    public ResponseEntity<Void> addMember(@AuthenticationPrincipal JWTUserData loggedUser,
                                          @PathVariable Long groupId,
                                          @Valid @RequestBody AddMemberRequest request){
        groupService.addMember(groupId, request.email());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PreAuthorize("@groupSecurity.isGroupAdmin(#loggedUser.userId, #groupId)")
    @GetMapping("/{groupId}/invitation")
    @Operation(
            summary = "Retorna token de convite do grupo",
            description = "Retorna token de convite do grupo que pode ser utilizado para compartilhar com outros usuarios para ingressarem no grupo via token"
    )
    public ResponseEntity<String> getInviteToken(@AuthenticationPrincipal JWTUserData loggedUser,
                                                 @PathVariable Long groupId){

        String inviteToken = groupService.getInviteToken(groupId);

        return ResponseEntity.ok(inviteToken);
    }

    @PostMapping("/join/{inviteToken}")
    @Operation(
            summary = "Envia token para ingressar no grupo",
            description = "Utiliza o token compartilhado para que o usuario possa ingressar no grupo"
    )
    public ResponseEntity<GenericGroupResponse> enterByToken(@AuthenticationPrincipal JWTUserData loggedUser, @PathVariable String inviteToken){
        GroupMember membership = groupService.joinGroupWithCode(loggedUser.userId(), inviteToken);
        GenericGroupResponse response = GenericGroupResponse.from(membership.getGroup(), membership.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping("/summary")
    @Operation(
            summary = "Retorna lista de grupos",
            description = "Retorna uma lista de grupos com dados basicos para visualização no display do usuario."
    )
    public ResponseEntity<List<GroupSummaryResponse>> listGroups(@AuthenticationPrincipal JWTUserData loggedUser){
        List<GroupSummaryResponse> summary = groupService.getMyGroupsSummary(loggedUser.userId());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/{groupId}")
    @Operation(
            summary = "Detalhes completos do grupo",
            description = "Retorna os metadados do grupo e a lista detalhada de seus integrantes.")
    public ResponseEntity<GroupDetailedResponse> getGroupDetails(@AuthenticationPrincipal JWTUserData loggedUser,
                                                                 @PathVariable Long groupId){
        return ResponseEntity.ok(groupService.getGroupDetails(loggedUser.userId(), groupId));
    }


    @PreAuthorize("@groupSecurity.isGroupAdmin(#loggedUser.userId, #groupId)")
    @PutMapping("/{groupId}")
    @Operation(
            summary = "Edita dados do grupo",
            description = "Edita dados estáticos do grupo.")
    public ResponseEntity<Void> editGroup(@AuthenticationPrincipal JWTUserData loggedUser,
                                          @PathVariable Long groupId, @RequestBody CreateGroupRequest request){
       groupService.updateGroup(groupId, request);
       return ResponseEntity.ok().build();
    }


    @PreAuthorize("@groupSecurity.isGroupAdmin(#loggedUser.userId, #groupId)")
    @DeleteMapping("/{groupId}")
    @Operation(
            summary = "Deleta o grupo",
            description = "Deleta todos o grupo completo do banco de dados, juntamente com os valores criados no grupo." +
                        "Caso o grupo ainda tenha valores em aberto, este nao pode ser deletado até os valores serem quitados.")
    public ResponseEntity<Void> deleteGroup(@AuthenticationPrincipal JWTUserData loggedUser,
                                            @PathVariable Long groupId){
        groupService.deleteGroup(loggedUser.userId(), groupId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{groupId}/leave")
    @Operation(summary = "Sair do grupo", description = "Remove o usuário do grupo. Se for Admin, transfere o cargo para o membro mais antigo.")
    public ResponseEntity<Void> leaveGroup(@AuthenticationPrincipal JWTUserData loggedUser,
                                           @PathVariable Long groupId) {
        groupService.leaveGroup(loggedUser.userId(), groupId);
        return ResponseEntity.ok().build();
    }
}
