package com.wisedu.next.api.domains

case class SmsNumSendResultResponse(err_code: Option[String], model: Option[String], success: Boolean, msg: Option[String])

case class AlibabaAliqinFcSmsNumSendResultResponse(result: SmsNumSendResultResponse)

case class AlibabaAliqinFcSmsErrorResponse(code: Option[Int], msg: Option[String], sub_code: Option[String], sub_msg: Option[String], request_id: Option[String])

case class AlibabaAliqinFcSmsNumSendResponse(alibaba_aliqin_fc_sms_num_send_response: Option[AlibabaAliqinFcSmsNumSendResultResponse], error_response: Option[AlibabaAliqinFcSmsErrorResponse], request_id: Option[String])