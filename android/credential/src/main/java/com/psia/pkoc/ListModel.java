package com.psia.pkoc;

import java.util.Date;

public class ListModel
{
    // private variables
    private boolean IsBusy;
    private String Name;
    private String Address;
    private int Rssi;
    private int Icon;
    private Date LastSeen;


    // getters
    public boolean getIsBusy()
    {
        return IsBusy;
    }
    public String getName()
    {
        return Name;
    }
    public String getAddress()
    {
        return Address;
    }
    public int getRssi()
    {
        return Rssi;
    }
    public Date getLastSeen()
    {
        return LastSeen;
    }
    public int getIconID()
    {
        return Icon;
    }

    // setters
    public void setIsBusy(boolean isBusy)
    {
        IsBusy = isBusy;
    }

    public void setName(String _Name)
    {
        Name = _Name;
    }
    public void setAddress(String _Address)
    {
        Address = _Address;
    }
    public void setRssi(int _Rssi)
    {
        Rssi = _Rssi;
    }
    public void setIcon(int _IconID)
    {
        Icon = _IconID;
    }
    public void setLastSeen(Date _LastSeen)
    {
        LastSeen = _LastSeen;
    }
}
