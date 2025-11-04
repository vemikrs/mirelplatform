package ${packageGroup}.${applicationId}.domain.service;

import ${packageGroup}.${applicationId}.domain.mapper.${serviceId.ucc()}Mapper;
import ${packageGroup}.${applicationId}.domain.model.${serviceId.ucc()}${eventId.ucc()}ParamModel;
import ${packageGroup}.${applicationId}.domain.model.${serviceId.ucc()}${eventId.ucc()}ResultModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ${serviceName}サービス.
 *
 * @author ${author}
 * @version ${version}
 */
@Service
@Transactional
public class ${serviceId.ucc()}Service {

  /**
   * ${serviceName}マッパー.
   */
  @Autowired
  private ${serviceId.ucc()}Mapper mapper;

  /**
   * ${eventName}.
   *
   * @param param ${serviceId.ucc()}${eventId.ucc()}ParamModel
   * @return ${serviceId.ucc()}${eventId.ucc()}ResultModel
   */
  @Transactional
  public ${serviceId.ucc()}${eventId.ucc()}ResultModel ${eventId}(${serviceId.ucc()}${eventId.ucc()}ParamModel param) {
    // TODO 実装
    ${serviceId.ucc()}${eventId.ucc()}ResultModel result = new ${serviceId.ucc()}${eventId.ucc()}ResultModel();
    return result;
  }
}
