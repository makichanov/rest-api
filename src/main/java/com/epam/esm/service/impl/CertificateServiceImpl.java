package com.epam.esm.service.impl;

import com.epam.esm.converter.CertificateToCertificateDtoConverter;
import com.epam.esm.repository.Specification;
import com.epam.esm.repository.api.CertificateRepository;
import com.epam.esm.repository.api.CertificateTagRepository;
import com.epam.esm.repository.api.TagRepository;
import com.epam.esm.exception.EntityNotCreatedException;
import com.epam.esm.exception.EntityNotFoundException;
import com.epam.esm.exception.EntityNotUpdatedException;
import com.epam.esm.model.dto.CertificateDto;
import com.epam.esm.model.dto.CreatingCertificateDto;
import com.epam.esm.model.dto.TagDto;
import com.epam.esm.model.dto.UpdatingCertificateDto;
import com.epam.esm.model.entity.Certificate;
import com.epam.esm.model.entity.OrderType;
import com.epam.esm.model.entity.SortType;
import com.epam.esm.model.entity.Tag;
import com.epam.esm.repository.specification.SpecificationCreator;
import com.epam.esm.service.api.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository<Long> certificateRepository;
    private final TagRepository<Long> tagRepository;
    private final CertificateTagRepository<Long> certificateTagRepository;

    @Override
    @Transactional
    public CertificateDto create(CreatingCertificateDto certificate) {
        Long id = certificateRepository.create(certificate);
        if (certificate.getTags() != null) {
            for (TagDto tag : certificate.getTags()) {
                Optional<Tag> toAdd = tagRepository.findByName(tag.getName());
                if (!toAdd.isPresent()) {
                    Long tagId = tagRepository.create(tag);
                    tag.setId(tagId);
                }
                Tag t = toAdd.get();
                tag.setId(t.getId());
            }
            boolean tagsAdded = certificateTagRepository.addTags(id, certificate.getTags());
            if (!tagsAdded) {
                throw new EntityNotCreatedException("Entity creating transaction failed on tags creation");
            }
        }
        Optional<Certificate> created = certificateRepository.findById(id);
        Certificate item = created.orElseThrow(() -> new EntityNotCreatedException("Cannot find created entity"));
        CertificateToCertificateDtoConverter converter = new CertificateToCertificateDtoConverter();
        return converter.convert(item);
    }

    @Override
    public List<CertificateDto> findAll(String tagName, String name, String description, SortType sort, OrderType order) {
        SpecificationCreator specificationCreator = new SpecificationCreator();
        Specification findSpecification =
                specificationCreator.createSpecification(name, description, tagName, sort, order);
        List<Certificate> certificates = certificateRepository.findBySpecification(findSpecification);
        for (Certificate c : certificates) {
            List<Tag> tags = tagRepository.findByCertificateId(c.getId());
            c.setTags(tags);
        }
        CertificateToCertificateDtoConverter converter = new CertificateToCertificateDtoConverter();
        return certificates.stream()
                .map(converter::convert)
                .collect(Collectors.toList());
    }

    @Override
    public CertificateDto findById(Long id) {
        Optional<Certificate> certificate = certificateRepository.findById(id);
        Certificate item = certificate.orElseThrow(
                () -> new EntityNotFoundException("Cannot find certificate with id " + id));
        List<Tag> certificateTags = tagRepository.findByCertificateId(item.getId());
        item.setTags(certificateTags);
        CertificateToCertificateDtoConverter converter = new CertificateToCertificateDtoConverter();
        return converter.convert(item);
    }

    @Override
    @Transactional
    public CertificateDto update(Long updateId, UpdatingCertificateDto replacement) {
        Timestamp currentTime = Timestamp.valueOf(ZonedDateTime.now().toLocalDateTime());
        replacement.setLastUpdateDate(currentTime);
        boolean updated = certificateRepository.update(updateId, replacement);
        if (!updated) {
            throw new EntityNotUpdatedException("Update wasn't carried out");
        }
        if (replacement.getTags() != null) {
            for (TagDto tag : replacement.getTags()) {
                Optional<Tag> t = tagRepository.findByName(tag.getName());
                if (!t.isPresent()) {
                    Long id = tagRepository.create(tag);
                    tag.setId(id);
                } else {
                    tag.setId(t.get().getId());
                }
            }
            certificateTagRepository.clearTags(updateId);
            boolean added = certificateTagRepository.addTags(updateId, replacement.getTags());
            if (!added) {
                throw new EntityNotUpdatedException("Failed to update entity: error while updating tags");
            }
        }
        return findById(updateId);
    }

    @Override
    public CertificateDto delete(Long deleteId) {
        CertificateDto old = findById(deleteId);
        boolean deleted = certificateRepository.delete(deleteId);
        if (!deleted) {
            throw new EntityNotUpdatedException("Delete wasn't carried out");
        }
        return old;
    }
}
