/**
 * 
 */
package org.selurgniman.bukkit.ohnoez;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.avaje.ebean.validation.NotNull;

/**
 * @author <a href="mailto:e83800@wnco.com">Chris Bandy</a>
 * Created on: Dec 14, 2011
 */
@Entity()
@Table(name = "ohshit_drops")
public class Drop {

	@Id
	private Integer id;

	@ManyToOne(cascade=CascadeType.REFRESH)
	@JoinColumn(name="drops")
	private Credits credit;
	
	@NotNull
    private Integer itemId;
    
    @NotNull
    private Integer itemCount;

    public Drop(){
    	this(0,0);
    }
    
    public Drop(int itemId, int itemCount){
    	this.itemId=itemId;
    	this.itemCount=itemCount;
    }
	
    /**
	 * @return the credit
	 */
	public Credits getCredit() {
		return credit;
	}

	/**
	 * @param credit the credit to set
	 */
	public void setCredit(Credits credit) {
		this.credit = credit;
	}

	
	/**
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}
	
	/**
	 * @return the itemId
	 */
	public Integer getItemId() {
		return itemId;
	}

	/**
	 * @param itemId the itemId to set
	 */
	public void setItemId(Integer itemId) {
		this.itemId = itemId;
	}

	/**
	 * @return the itemCount
	 */
	public Integer getItemCount() {
		return itemCount;
	}

	/**
	 * @param itemCount the itemCount to set
	 */
	public void setItemCount(Integer itemCount) {
		this.itemCount = itemCount;
	}
}
