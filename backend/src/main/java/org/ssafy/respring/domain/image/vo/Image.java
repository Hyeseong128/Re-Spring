package org.ssafy.respring.domain.image.vo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.ssafy.respring.domain.post.vo.Post;
import org.ssafy.respring.domain.story.vo.Story;

@Entity
@Table(name = "image")
@Getter @Setter
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long imageId;

    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne
    @JoinColumn(name = "story_id")
    private Story story;

    @PrePersist
    @PreUpdate
    public void validateAssociations() {
        if (post != null && story != null) {
            throw new IllegalStateException("An image cannot be associated with both a post and a story.");
        }
    }

}
