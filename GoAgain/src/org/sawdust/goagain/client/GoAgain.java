package org.sawdust.goagain.client;

import org.sawdust.goagain.shared.GameData;
import org.sawdust.goagain.shared.GameId;
import org.sawdust.goagain.shared.GameRecord;
import org.sawdust.goagain.shared.GoGame;
import org.sawdust.goagain.shared.GoAI;
import org.sawdust.goagain.shared.GreetingService;
import org.sawdust.goagain.shared.GreetingServiceAsync;
import org.sawdust.goagain.shared.Tile;

import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class GoAgain implements EntryPoint {

  public static final GreetingServiceAsync service = GWT.create(GreetingService.class);

  public static DialogBox showDialog(Label... widgets) {
    final DialogBox dialogBox = new DialogBox();
    VerticalPanel w = new VerticalPanel();
    for(Label widget : widgets) w.add(widget);
    Button close = new Button("Close");
    close.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        dialogBox.hide();
      }
    });
    w.add(close);
    dialogBox.add(w);
    dialogBox.center();
    dialogBox.show();
    return dialogBox;
  }

  public GameId gameId;
  private boolean aiEnabled = true;
  private boolean autoplay = false;
  private GameData data;
  private int height;
  private Widget infoWidget; 
  private int width;
  
  private final GoBoard board = new GoBoard();
  private final Canvas canvas = Canvas.createIfSupported();
  private final SplitLayoutPanel basePanel = new SplitLayoutPanel();

  protected void addControl(VerticalPanel vpanel, final Button button, ClickHandler handler) {
    button.setWidth("100%");
    button.addClickHandler(handler);
    vpanel.add(button);
    vpanel.setCellHeight(button, button.getOffsetHeight() + "px");
  }

  private void addHandlers() {
    canvas.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        Tile nearestTile = data.game.nearestTile(event.getX(),event.getY(), width, height);
        data.game.occupy(nearestTile, data.game.currentPlayer);
        update();
        if(aiEnabled) aiAsync();
      }
    });
    Window.addResizeHandler(new ResizeHandler() {
      public void onResize(ResizeEvent event) {
        height = event.getHeight();
        width = event.getWidth();
        basePanel.setHeight(height + "px");
        canvas.setHeight(height + "px");
        canvas.setCoordinateSpaceHeight(height);
        canvas.setCoordinateSpaceWidth(width);
        infoWidget.setHeight(height + "px");
        update();
      }
    });
  }
  
  protected void aiAsync() {
    if(!aiEnabled) return;
    final GoAI goAI = data.ai[data.game.currentPlayer-1];
    if(goAI.useServer && !GoAI.isServer)
    {
      service.move(data.game, goAI, new AsyncCallback<GoGame>() {
        public void onFailure(Throwable caught) {
          caught.printStackTrace();
          showDialog(new Label("Server Error"), new Label(caught.getMessage()));
        }
        
        public void onSuccess(GoGame result) {
          data.game = result;
          update();
          if(null == data.game.winner && autoplay) aiAsync();
        }
      });
    }
    else
    {
      new Timer() {
        @Override
        public void run() {
          goAI.move(data.game);
          update();
          if(null == data.game.winner && autoplay) aiAsync();
        }
      }.schedule(1);
    }
  }

  private Widget getInfoWidget(SplitLayoutPanel layoutPanel) {
    VerticalPanel vpanel = new VerticalPanel();
    vpanel.setWidth("100%");
    vpanel.setHeight(height + "px");
    vpanel.setSpacing(0);
    layoutPanel.addEast(vpanel, 200);

    {
      final Button button = new Button("Reset Game");
      addControl(vpanel, button, new ClickHandler() {
        public void onClick(ClickEvent event) {
          data.game.reset();
          update();
        }
      });
    }
    
    {
        final Button button = new Button("Demo " + (autoplay?"ON":"OFF"));
        addControl(vpanel, button, new ClickHandler() {
          public void onClick(ClickEvent event) {
            autoplay = !autoplay;
            button.setText("Demo " + (autoplay?"ON":"OFF"));
          }
        });
    }
    
    {
      final Button button = new Button("Computer Player " + (aiEnabled?"ON":"OFF"));
      addControl(vpanel, button, new ClickHandler() {
        public void onClick(ClickEvent event) {
          aiEnabled = !aiEnabled;
          button.setText("Computer Player " + (aiEnabled?"ON":"OFF"));
        }
      });
    }
    

    {
      final Button button = new Button("Configure Game");
      addControl(vpanel, button, new ClickHandler() {
        public void onClick(ClickEvent event) {
          final DialogBox dialogBox = new DialogBox();
          VerticalPanel dialogVPanel = new VerticalPanel();
          dialogBox.add(dialogVPanel);
          
          dialogVPanel.add(board.getConfigWidget(data.game));

          Button close = new Button("OK");
          dialogVPanel.add(close);
          close.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
              dialogBox.hide();
              update();
            }
          });
          
          dialogBox.center();
          dialogBox.show();
        }
      });
    }

    {
      final Button button = new Button("Black AI");
      addControl(vpanel, button, new ClickHandler() {
        public void onClick(ClickEvent event) {
          final DialogBox dialogBox = new DialogBox();
          VerticalPanel dialogVPanel = new VerticalPanel();
          dialogBox.add(dialogVPanel);
          
          dialogVPanel.add(GoAiConfig.getConfigWidget(data.ai[0]));

          Button close = new Button("OK");
          dialogVPanel.add(close);
          close.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
              dialogBox.hide();
              update();
            }
          });
          
          dialogBox.center();
          dialogBox.show();
        }
      });
    }

    {
      final Button button = new Button("White AI");
      addControl(vpanel, button, new ClickHandler() {
        public void onClick(ClickEvent event) {
          final DialogBox dialogBox = new DialogBox();
          VerticalPanel dialogVPanel = new VerticalPanel();
          dialogBox.add(dialogVPanel);
          
          dialogVPanel.add(GoAiConfig.getConfigWidget(data.ai[1]));

          Button close = new Button("OK");
          dialogVPanel.add(close);
          close.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
              dialogBox.hide();
              update();
            }
          });
          
          dialogBox.center();
          dialogBox.show();
        }
      });
    }

    Widget boardInfo = board.getInfo();
    vpanel.add(boardInfo);
    int h = vpanel.getOffsetHeight() - boardInfo.getAbsoluteTop();
    vpanel.setCellHeight(boardInfo, h + "px");
    //vpanel.setCellVerticalAlignment(boardInfo, VerticalPanel.ALIGN_MIDDLE);
    
    return vpanel;
  }

  protected void init() {
    RootPanel.get().add(basePanel);
    
    height = Window.getClientHeight();
    width = Window.getClientWidth();
    basePanel.setWidth("100%");
    basePanel.setHeight(height + "px");
    
    
    infoWidget = getInfoWidget(basePanel);
    
    
    if (canvas == null) {
      RootPanel.get().add(new Label("HTML Canvas Element not supported"));
      return;
    }
    canvas.setHeight(height + "px");
    canvas.setCoordinateSpaceWidth(width);
    canvas.setCoordinateSpaceHeight(height);
    basePanel.add(canvas);
    
    addHandlers();
    update();
  }
  
  protected void newGame() {
    data = new GameData();
    data.game = new GoGame();
    data.ai = new GoAI[]{new GoAI(), new GoAI()};
    gameId = null;
    service.newGame(data, new AsyncCallback<GameId>() {
      public void onFailure(Throwable caught) {
        caught.printStackTrace();
        showDialog(new Label("Error Creating Game"), new Label(caught.getMessage()));
      }
      
      public void onSuccess(GameId result) {
        gameId = result;
        String queryString = Window.Location.getQueryString();
        if(queryString.contains("?"))
        {
          Window.Location.assign(queryString + "&gameId=" + gameId.key);
        }
        else
        {
          Window.Location.assign(queryString + "?gameId=" + gameId.key);
        }
      }
    });
  }
  
  public void onModuleLoad() {
    String key = Window.Location.getParameter("gameId");
    if(null == key)
    {
      newGame();
    }
    else
    {
      gameId = new GameId(key, 0);
      service.getGame(gameId, new AsyncCallback<GameRecord>() {
        public void onFailure(Throwable caught) {
          caught.printStackTrace();
          showDialog(new Label("Error Getting Game"), new Label(caught.getMessage())).addCloseHandler(new CloseHandler<PopupPanel>() {
            public void onClose(CloseEvent<PopupPanel> event) {
              newGame();
            }
          });
        }
        
        public void onSuccess(GameRecord result) {
          data = result.data;
          gameId = result.activeId;
          init();
        }
      });
    };
    
  }

  private void update() {
    if(null != data.game.winner)
    {
      final DialogBox dialogBox = new DialogBox();
      VerticalPanel dialogVPanel = new VerticalPanel();
      dialogBox.add(dialogVPanel);
      
      HTML w = new HTML();
      w.setText((data.game.winner==0?"Black":"White")+" Player Won!");
      dialogVPanel.add(w);

      Button close = new Button("OK");
      dialogVPanel.add(close);
      close.addClickHandler(new ClickHandler() {
        public void onClick(ClickEvent event) {
          data.game.reset();
          dialogBox.hide();
          update();
        }
      });
      
      dialogBox.center();
      dialogBox.show();
    }
    Context2d context = canvas.getContext2d();
    context.clearRect(0, 0, width, height);
    board.draw(data.game, context, width, height);
    
    service.saveGame(gameId, data, new AsyncCallback<GameId>() {
      public void onSuccess(GameId result) {
        gameId = result;
      }
      
      public void onFailure(Throwable caught) {
        caught.printStackTrace();
        showDialog(new Label("Save Game Failed"), new Label(caught.getMessage()));
      }
    });
  }

  protected void updateAsync() {
    new Timer() {
      @Override
      public void run() {
        update();
      }
    }.schedule(1);
  }
}