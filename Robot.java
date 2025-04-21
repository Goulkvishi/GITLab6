package brokenrobotgame.model;

import brokenrobotgame.model.doors.AbstractDoor;
import brokenrobotgame.model.doors.AbstractDoorWithLock;
import brokenrobotgame.model.events.RobotActionEvent;
import brokenrobotgame.model.events.RobotActionListener;
import brokenrobotgame.model.navigation.Direction;
import brokenrobotgame.model.navigation.MiddlePosition;
import brokenrobotgame.model.navigation.CellPosition;
import java.util.ArrayList;


/*
 * Robot - can move on the field if there is charge in its internal battery; 
 * it determines on its own where it can walk; can use batteries
 * that are on the field
 */
public class UberRobot
{
    // ------------------- Establishing connection with the game field -----------------
    private GameField _field;

    public GameField field() {
        return _field;
    }

    public void unsetField() {
        if (_field != null) {
            GameField field = _field;
            _field = null;
            field.unsetRobot();
        }
    }

    private void checkField() {
        if (_field == null) {
            throw new NullPointerException("The robot does not belong to a field. It has been destroyed.");
        }
    }

    public Robot(GameField field, CellPosition startPos) {

        if (field == null) {
            throw new NullPointerException("field is null.");
        }

        _position = startPos;

        if (!field.setRobot(this)) {
            throw new RuntimeException("Failed to bind the robot to the field.");
        }

        _field = field;
    }

    // ------------------- Robot "feeds" from a battery and can swap them -----------------
    private Battery _battery;

    public void useBattery(Battery outBattery) {

        checkField();

        if (outBattery == null || (outBattery.position() != null || !outBattery.position().equals(_position))) {
            throw new IllegalStateException("The battery must be at the robot's position or outside the field.");
        }

        if (_battery != null) {
            _battery.destroy();
        }

        _battery = outBattery;

        if (outBattery.position() != null) {
            _field.removeBattery(outBattery);
        }

        fireRobotAction();
    }
	
    public int amountOfCharge(){
        return _battery != null ? _battery.amountOfCharge() : 0;
    }

    public int chargeCapacity(){
        return _battery != null ? _battery.chargeCapacity() : 0;
    }
    
    protected void reduceCharge(int delta){
        if (_battery == null) {
            throw new IllegalArgumentException("Battery is missing.");
        }
        _battery.reduceCharge(delta);
    }
	
    
    // ------------------- Robot can open and close doors -----------------

    public boolean openCloseDoor(Direction direct){

        checkField();

        if (amountOfCharge() > 0) { 
            MiddlePosition doorPos = new MiddlePosition(_position, direct);
            AbstractDoor door = _field.door(doorPos);

            if (door != null) {
                if (door.isOpen()) {
                    door.close();
                } else {
                    if(!door.open()) return false;
                }

                reduceCharge(5);

                fireRobotAction();
                return true;
            }
        }
        return false;
    }

    public boolean unlockDoor(Direction direction, String code){

        checkField();

        MiddlePosition doorPos = new MiddlePosition(_position, direction);  // save both options
        AbstractDoor door = _field.door(doorPos);

        if (door instanceof AbstractDoorWithLock) {

            if (((AbstractDoorWithLock) door).unlock(code)){
                fireRobotAction();
                return true;
            }
        }
        return false;
    }
	
	// ------------------- Robot can break walls -----------------
	
	public boolean breakWall(Direction direct){

        checkField();

        if (amountOfCharge() >= 6) { 
            MiddlePosition wallPos = new MiddlePosition(_position, direct);
			
			boolean success = _field.delWall(wallPos);

            if (success) {

                reduceCharge(6);

                fireRobotAction();
                return true;
            }
        }
        return false;
    }
    
    // ------------------- Robot's position -----------------

    private CellPosition _position;
    
    public CellPosition position(){
        return _position;
    }
    
    protected boolean setPosition(CellPosition pos){
        _position = pos;
        return true;
    }
	

    // ------------------- Move the robot -----------------

    public void makeMove(Direction direct) {
        if (amountOfCharge() > 0)
        {
            if (moveIsPossible(direct)) 
            {
                setPosition(position().next(direct));
                reduceCharge(3);

                fireRobotAction();
            }
            else {
                reduceCharge(4);

                Direction oppositeDirect = direct.opposite();

                if (moveIsPossible(oppositeDirect))
                {
                    setPosition(position().next(oppositeDirect));
                }
                fireRobotAction();
            }
        }
    }

    private boolean moveIsPossible(Direction direct) {

        checkField();

        if (!position().hasNext(direct)) return false;

        MiddlePosition nextMiddlePos = new MiddlePosition(position(), direct);

        if (_field.isWall(nextMiddlePos)) return false;

        AbstractDoor door = _field.door(nextMiddlePos);
        if (door != null && !door.isOpen()) return false;

        return true;
    }
    
    // ---------------------- Generates events -----------------------------

    private ArrayList<RobotActionListener> _listeners = new ArrayList<>();
    
    public void addRobotActionListener(RobotActionListener l) {
        _listeners.add(l);
    }

    public void removeRobotActionListener(RobotActionListener l) {
        _listeners.remove(l);
    }

    protected void fireRobotAction() {
        RobotActionEvent event = new RobotActionEvent(this);
        for (RobotActionListener listener : _listeners) {
            listener.robotMakedMove(event);
        }
    }
}
