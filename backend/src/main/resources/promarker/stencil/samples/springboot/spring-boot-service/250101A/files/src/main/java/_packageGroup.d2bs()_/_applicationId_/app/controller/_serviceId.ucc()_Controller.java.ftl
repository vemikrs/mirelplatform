package ${packageGroup}.${applicationId}.app.controller;

import ${packageGroup}.${applicationId}.app.request.${serviceId.ucc()}${eventId.ucc()}Request;
import ${packageGroup}.${applicationId}.app.response.MessageResponse;
import ${packageGroup}.${applicationId}.app.response.${serviceId.ucc()}${eventId.ucc()}Response;
import ${packageGroup}.${applicationId}.domain.model.${serviceId.ucc()}${eventId.ucc()}ParamModel;
import ${packageGroup}.${applicationId}.domain.model.${serviceId.ucc()}${eventId.ucc()}ResultModel;
import ${packageGroup}.${applicationId}.domain.service.${serviceId.ucc()}Service;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ${serviceName}コントローラ.
 *
 * @author ${author}
 * @version ${version}
 */
@Slf4j
@RestController
@EnableWebSecurity
@RequestMapping("/${serviceId.ucc()}")
public class ${serviceId.ucc()}Controller {

  /**
   * ${serviceName}サービス.
   */
  @Autowired
  private ${serviceId.ucc()}Service service;
  /**
   * モデルマッパー.
   */
  @Autowired
  private ModelMapper modelMapper;

  /**
   * ${eventName}.
   *
   * @param request  ${serviceId.ucc()}${eventId.ucc()}Request
   * @return ResponseEntity<?> レスポンス
   */
  @PostMapping
  public ResponseEntity<?> ${eventId}(@RequestBody @Validated final ${serviceId.ucc()}${eventId.ucc()}Request request) {
    ${serviceId.ucc()}${eventId.ucc()}ParamModel param = modelMapper.map(request, ${serviceId.ucc()}${eventId.ucc()}ParamModel.class);
    ${serviceId.ucc()}${eventId.ucc()}ResultModel result = service.${eventId}(param);
    ${serviceId.ucc()}${eventId.ucc()}Response response = modelMapper.map(result, ${serviceId.ucc()}${eventId.ucc()}Response.class);
    return ResponseEntity.ok(response);
  }
}
