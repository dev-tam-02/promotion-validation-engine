package vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import vn.viettel.vds.promotion.validation.engine.adapter.out.persistence.mongo.document.RuleMongoEntity;

import java.util.List;

@Repository
public interface RuleMongoRepository extends MongoRepository<RuleMongoEntity, String> {

    List<RuleMongoEntity> findByEnabledTrue();
}