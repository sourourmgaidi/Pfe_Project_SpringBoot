package tn.iset.investplatformpfe.DTO;

import lombok.Data;

@Data
public class AttachmentDTO {
    private Long id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String downloadUrl;
}
