package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.mapper;

import org.mapstruct.Mapper;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.RuleMongoEntity;
import vn.viettel.vds.promotion.validation.engine.domain.model.Rule;

@Mapper(componentModel = "spring")
public interface RuleMapper {

    Rule toDomain(RuleMongoEntity entity);

    RuleMongoEntity toEntity(Rule rule);
}