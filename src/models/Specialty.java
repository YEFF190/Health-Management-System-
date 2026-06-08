package models;

public class Specialty {
    private int    specialtyId;
    private String name;
    private int    groupId;

    public Specialty() {}

    public Specialty(int specialtyId, String name, int groupId) {
        this.specialtyId = specialtyId;
        this.name        = name;
        this.groupId     = groupId;
    }

    public int    getSpecialtyId()              { return specialtyId; }
    public void   setSpecialtyId(int id)        { this.specialtyId = id; }
    public String getName()                     { return name; }
    public void   setName(String name)          { this.name = name; }
    public int    getGroupId()                  { return groupId; }
    public void   setGroupId(int groupId)       { this.groupId = groupId; }

    /** Used by ComboBox display */
    @Override
    public String toString() { return name; }
}