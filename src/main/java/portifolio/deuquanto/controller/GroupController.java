package portifolio.deuquanto.controller;

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
import portifolio.deuquanto.entity.Group;
import portifolio.deuquanto.entity.GroupMember;
import portifolio.deuquanto.entity.GroupRole;
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
    public ResponseEntity<CreateGroupResponse> createGroup(@AuthenticationPrincipal JWTUserData loggedUser,
                                                           @Valid @RequestBody CreateGroupRequest request){

        Group createdGroup = groupService.createGroup(loggedUser.userId(), request);

        CreateGroupResponse response = CreateGroupResponse.from(createdGroup, GroupRole.ADMIN);

        return ResponseEntity.status(HttpStatusCode.valueOf(201))
                .body(response);
    }

    @GetMapping("/member")
    public ResponseEntity<List<GenericGroupResponse>> getMembersGroup(@AuthenticationPrincipal JWTUserData loggedUser){
        List<GroupMember> membership = groupService.getAllMembersGroups(loggedUser.userId());

        List<GenericGroupResponse> responseList = membership.stream()
                .map(gm -> GenericGroupResponse.from(gm.getGroup(), gm.getRole()))
                .toList();

        return ResponseEntity.ok(responseList);
    }


    @PreAuthorize("@groupSecurity.isGroupAdmin(#loggedUser.userId, #groupId)")
    @PostMapping("/{groupId}/member/new")
    public ResponseEntity<Void> addMember(@AuthenticationPrincipal JWTUserData loggedUser,
                                          @PathVariable Long groupId,
                                          @Valid @RequestBody AddMemberRequest request){
        groupService.addMember(groupId, request.email());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PreAuthorize("@groupSecurity.isGroupAdmin(#loggedUser.userId, #groupId)")
    @GetMapping("/{groupId}/invitation")
    public ResponseEntity<String> getInviteToken(@AuthenticationPrincipal JWTUserData loggedUser,
                                                 @PathVariable Long groupId){

        String inviteToken = groupService.getInviteToken(groupId);

        return ResponseEntity.ok(inviteToken);
    }

    @PostMapping("/join/{inviteToken}")
    public ResponseEntity<GenericGroupResponse> enterByToken(@AuthenticationPrincipal JWTUserData loggedUser, @PathVariable String inviteToken){
        GroupMember membership = groupService.joinGroupWithCode(loggedUser.userId(), inviteToken);
        GenericGroupResponse response = GenericGroupResponse.from(membership.getGroup(), membership.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


}
