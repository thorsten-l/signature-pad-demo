package l9g.webapp.signaturepaddemo.ws;

import l9g.webapp.signaturepaddemo.dto.DtoEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * WebSocket handler to send HeartbeatDTO every 5 seconds.
 */
@Slf4j
@RequiredArgsConstructor
public class SignaturePadWebSocketHandler implements WebSocketHandler
{
  @Getter
  private final Map<String, WebSocketSession> sessionsBySessionId = new HashMap<>();
  
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Invoked after a new WebSocket connection has been established.
   *
   * @param session the WebSocket session that has been established
   *
   * @throws Exception if an error occurs during the establishment of the connection
   */
  @Override
  public void afterConnectionEstablished(WebSocketSession session)
    throws Exception
  {
    String padUuid = (String)session.getAttributes().get(SignaturePadWebSocketConfig.SIGNATURE_PAD_UUID);
    log.debug("afterConnectionEstablished: session id = {}/{}", session.getId(), padUuid);
    if(padUuid != null)
    {
      UUID uuid = UUID.fromString(padUuid);
      if(uuid.toString().equals(padUuid))
      {
        log.debug("store session");
        this.sessionsBySessionId.put(session.getId(), session);
      }
    }
  }

  /**
   * Handles incoming WebSocket messages.
   *
   * @param session the WebSocket session associated with the message
   * @param message the WebSocket message received
   *
   * @throws Exception if an error occurs while handling the message
   */
  @Override
  public void handleMessage(WebSocketSession session,
    org.springframework.web.socket.WebSocketMessage<?> message)
    throws Exception
  {
    log.debug("handleMessage ({}) message.payload={}",
      session.getId(), message.getPayload().toString());
  }

  /**
   * Handles transport errors that occur during WebSocket communication.
   *
   * @param session the WebSocket session where the error occurred
   * @param exception the exception that was thrown
   *
   * @throws Exception if an error occurs while handling the transport error
   */
  @Override
  public void handleTransportError(WebSocketSession session,
    Throwable exception)
    throws Exception
  {
    log.error("handleTransportError: session id = {}, error: {}",
      session.getId(), exception.getMessage());
    session.close();
    sessionsBySessionId.remove(session.getId());
  }

  /**
   * Invoked after a WebSocket connection has been closed.
   *
   * @param session the WebSocket session that was closed
   * @param closeStatus the status object containing the code and reason for the closure
   *
   * @throws Exception if any error occurs during the handling of the closed connection
   */
  @Override
  public void afterConnectionClosed(WebSocketSession session,
    org.springframework.web.socket.CloseStatus closeStatus)
    throws Exception
  {
    log.debug("afterConnectionClosed {} status {}/{}",
      session.getId(), closeStatus.getCode(), closeStatus.getReason());
    sessionsBySessionId.remove(session.getId());
  }

  /**
   * Indicates whether this WebSocket handler supports partial messages.
   *
   * @return false, indicating that partial messages are not supported.
   */
  @Override
  public boolean supportsPartialMessages()
  {
    return false;
  }

  /**
   * Fires an event to all open WebSocket sessions.
   *
   * @param event the event to be sent to the WebSocket sessions
   *
   * @throws IOException if an I/O error occurs while sending the message
   */
  public void fireEventToAllSessions(DtoEvent event)
    throws IOException
  {
    log.trace("fireEvent to {} sessions", sessionsBySessionId.size());
    
    sessionsBySessionId.forEach((id, session) ->
    {
      if(session == null ||  ! session.isOpen())
      {
        sessionsBySessionId.remove(id);
      }
    });
    
    for(WebSocketSession session : sessionsBySessionId.values())
    {
      if(session != null && session.isOpen())
      {
        String json = objectMapper.writeValueAsString(event);
        session.sendMessage(new TextMessage(json));
        log.trace("Sent text message: {}", json);
      }
    }
  }
  
  public void fireEventToPad(DtoEvent event, String padUuid)
    throws IOException
  {
    log.trace("fireEvent to pad {}", padUuid);
    sessionsBySessionId.values().forEach(session ->
    {
      if(session.isOpen() && padUuid.equals((String)session.getAttributes().get(SignaturePadWebSocketConfig.SIGNATURE_PAD_UUID)))
      {
        try
        {
          String json = objectMapper.writeValueAsString(event);
          session.sendMessage(new TextMessage(json));
          log.trace("Sent text message: {}", json);
        }
        catch(Exception ex)
        {
          log.error("can't send message", ex);
        }
      }
    });
  }
  
}
