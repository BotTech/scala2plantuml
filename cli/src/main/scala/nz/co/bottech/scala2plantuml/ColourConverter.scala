package nz.co.bottech.scala2plantuml

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.color.ANSIConstants._
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase

private[scala2plantuml] class ColourConverter extends ForegroundCompositeConverterBase[ILoggingEvent] {

  override def transform(event: ILoggingEvent, in: String): String =
    if (ColourConverter.enabled) super.transform(event, in)
    else in

  override def getForegroundColorCode(event: ILoggingEvent): String =
    event.getLevel.toInt match {
      case Level.ERROR_INT => RED_FG
      case Level.WARN_INT  => YELLOW_FG
      case Level.DEBUG_INT => BLUE_FG
      case Level.TRACE_INT => GREEN_FG
      case _               => DEFAULT_FG
    }
}

private[scala2plantuml] object ColourConverter {

  // TODO: How else can we programmatically control this?
  @volatile
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  var enabled: Boolean = true
}
