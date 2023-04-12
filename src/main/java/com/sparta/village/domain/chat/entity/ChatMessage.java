package com.sparta.village.domain.chat.entity;

import com.sparta.village.domain.user.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User sender;

    @Column(nullable = false)
    private String content;

    @ManyToOne
    private ChatRoom room;

//    @Column(nullable = false)
//    private MessageType type;
//
//    public enum MessageType {
//        ENTER, TALK, QUIT
//    }

    public ChatMessage(User sender, String content, ChatRoom room) {
        this.sender = sender;
        this.content = content;
        this.room = room;
    }
}
