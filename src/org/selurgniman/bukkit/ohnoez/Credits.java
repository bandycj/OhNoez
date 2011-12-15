/**
 * 
 */
package org.selurgniman.bukkit.ohnoez;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;

@Entity()
@Table(name = "ohshit_credits")
public class Credits {

    @Id
    private int id;
    
    @Length(max=30)
    @NotEmpty
    private String playerName;
    
    @NotNull
    private Integer credits;

	private Date lastCredit;
	
    @OneToMany(mappedBy="credit",cascade=CascadeType.ALL)
	private List<Drop> drops;

    public Credits(){
    	this.drops=new ArrayList<Drop>();
    }
    
    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String ply) {
        this.playerName = ply;
    }

    public Player getPlayer() {
        return Bukkit.getServer().getPlayer(playerName);
    }

    public void setPlayer(Player player) {
        this.playerName = player.getName();
    }

    public Integer getCredits(){
        return credits;
    }

    public void setCredits(Integer credits){
        this.credits = credits;
    }
    
	public Date getLastCredit() {
		return lastCredit;
	}

	public void setLastCredit(Date lastCredit) {
		this.lastCredit = lastCredit;
	}
	
	public List<Drop> getDrops() {
		return drops;
	}

	public void setDrops(List<Drop> drops) {
		this.drops = drops;
	}
}