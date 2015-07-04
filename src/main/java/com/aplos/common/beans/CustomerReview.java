package com.aplos.common.beans;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.model.SelectItem;

import com.aplos.common.FileDetailsOwnerHelper;
import com.aplos.common.LabeledEnumInter;
import com.aplos.common.SaveableFileDetailsOwnerHelper;
import com.aplos.common.annotations.DynamicMetaValueKey;
import com.aplos.common.annotations.persistence.Any;
import com.aplos.common.annotations.persistence.AnyMetaDef;
import com.aplos.common.annotations.persistence.Column;
import com.aplos.common.annotations.persistence.Entity;
import com.aplos.common.annotations.persistence.JoinColumn;
import com.aplos.common.annotations.persistence.ManyToOne;
import com.aplos.common.annotations.persistence.Transient;
import com.aplos.common.aql.BeanDao;
import com.aplos.common.enums.CommonWorkingDirectory;
import com.aplos.common.interfaces.FileDetailsOwnerInter;
import com.aplos.common.utils.ApplicationUtil;
import com.aplos.common.utils.CommonUtil;

@Entity
@ManagedBean
@SessionScoped
@DynamicMetaValueKey(oldKey="CUST_REVIEW")
public class CustomerReview extends AplosBean implements FileDetailsOwnerInter {
	private static final long serialVersionUID = 3275708465128621364L;
	private String reviewerFirstName;
	private String reviewerSurname;
	private String reviewTitle;
	@Column(columnDefinition="LONGTEXT")
	private String description;
	private String location;
	private String reviewerPosition;
	private String websiteUrl;
	private boolean isShowingOnWebsite;
	private BigDecimal score;

//	private String image1Url;
//	private String image2Url;
	
	@ManyToOne
	private FileDetails image1Details;
	@ManyToOne
	private FileDetails image2Details;

	@Any( metaColumn = @Column( name = "parent_type" ) )
    @AnyMetaDef( idType = "long", metaType = "string", metaValues = {
    		/* Meta Values added in at run-time */
    } )
    @JoinColumn(name="parent_id")
	private AplosBean parent;
	
	@Transient
	private CustomerReviewFdoh customerReviewFdoh = new CustomerReviewFdoh(this);

	public enum CustomerReviewSortType implements LabeledEnumInter {
		A_TO_Z ( "A to Z", new Comparator<CustomerReview>() {
			@Override
			public int compare(CustomerReview o1, CustomerReview o2) {
				return o1.getReviewTitle().compareTo(o2.getReviewTitle());
			}
		}),
		DATE_CREATED ("Date", new Comparator<CustomerReview>() {
			@Override
			public int compare(CustomerReview o1, CustomerReview o2) {
				if( o1.getDateCreated() == null ) {
					return -1;
				} else if( o2.getDateCreated() == null ) {
					return 1;
				} else {
					return o2.getDateCreated().compareTo(o1.getDateCreated());
				}
			}
		});

		private String label;
		private Comparator<CustomerReview> comparator;

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private CustomerReviewSortType( String label, Comparator comparator ) {
			this.label = label;
			this.comparator = comparator;
		}

		@Override
		public String getLabel() {
			return label;
		}

		public Comparator<CustomerReview> getComparator() {
			return comparator;
		}
	}

	public enum CustomerReviewImageKey {
		IMAGE_ONE,
		IMAGE_TWO;
	}
	
	@Override
	public String getDisplayName() {
		return getReviewTitle();
	};
	
	@Override
	public void saveBean(SystemUser currentUser) {
		FileDetails.saveFileDetailsOwner(this, CustomerReviewImageKey.values(), currentUser);
	}
	
	@Override
	public void superSaveBean(SystemUser currentUser) {
		super.saveBean(currentUser);
	}

	public List<SelectItem> getSortTypeSelectItems() {
		return CommonUtil.getEnumSelectItems( CustomerReviewSortType.class, null );
	}

	public List<CustomerReview> retrieveCustomerReviewList( AplosBean parent ) {
		return retrieveCustomerReviewListStatic(parent);
	}

	@SuppressWarnings("unchecked")
	public static List<CustomerReview> retrieveCustomerReviewListStatic( AplosBean parent ) {
		if( parent != null ) {
			BeanDao customerReviewDao = new BeanDao( CustomerReview.class );
			customerReviewDao.addWhereCriteria( "bean.parent.id = " + parent.getId() + " AND bean.parent.class = '" + ApplicationUtil.getClass(parent).getSimpleName() + "'" );
			return customerReviewDao.setIsReturningActiveBeans(true).getAll();
		} else {
			return new ArrayList<CustomerReview>();
		}
	}

	public String getReviewerFullName() {
		String fullName = getReviewerFirstName();
		if( !CommonUtil.getStringOrEmpty( fullName ).equals( "" ) &&
				!CommonUtil.getStringOrEmpty( getReviewerSurname() ).equals( "" ) ) {
			fullName += " " + getReviewerSurname();
		}
		return fullName;
	}

	public String getFullImage1Url( boolean addContextPath, boolean addRandom ) {
		return getImage1Details().getFullFileUrl(addContextPath);
	}

	public String getFullImage2Url( boolean addContextPath, boolean addRandom ) {
		return getImage2Details().getFullFileUrl(addContextPath);
	}

	public void setReviewerFirstName(String reviewerFirstName) {
		this.reviewerFirstName = reviewerFirstName;
	}
	public String getReviewerFirstName() {
		return reviewerFirstName;
	}
	public void setReviewerSurname(String reviewerSurname) {
		this.reviewerSurname = reviewerSurname;
	}
	public String getReviewerSurname() {
		return reviewerSurname;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription() {
		return description;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getLocation() {
		return location;
	}
	public void setReviewerPosition(String reviewerPosition) {
		this.reviewerPosition = reviewerPosition;
	}
	public String getReviewerPosition() {
		return reviewerPosition;
	}
	public void setWebsiteUrl(String websiteUrl) {
		this.websiteUrl = websiteUrl;
	}
	public String getWebsiteUrl() {
		return websiteUrl;
	}

	public void setShowingOnWebsite(boolean isShowingOnWebsite) {
		this.isShowingOnWebsite = isShowingOnWebsite;
	}

	public boolean isShowingOnWebsite() {
		return isShowingOnWebsite;
	}

	public void setReviewTitle(String reviewTitle) {
		this.reviewTitle = reviewTitle;
	}

	public String getReviewTitle() {
		return reviewTitle;
	}

	public void setParent(AplosBean parent) {
		this.parent = parent;
	}

	public AplosBean getParent() {
		return parent;
	}

	public void setScore(BigDecimal score) {
		this.score = score;
	}

	public BigDecimal getScore() {
		return score;
	}

	public FileDetails getImage1Details() {
		return image1Details;
	}

	public void setImage1Details(FileDetails image1Details) {
		this.image1Details = image1Details;
	}

	public FileDetails getImage2Details() {
		return image2Details;
	}

	public void setImage2Details(FileDetails image2Details) {
		this.image2Details = image2Details;
	}
	
	@Override
	public FileDetailsOwnerHelper getFileDetailsOwnerHelper() {
		return customerReviewFdoh;
	}
	
	private class CustomerReviewFdoh extends SaveableFileDetailsOwnerHelper {
		public CustomerReviewFdoh( CustomerReview customerReview ) {
			super( customerReview );
		}

		@Override
		public String getFileDetailsDirectory(String fileDetailsKey, boolean includeServerWorkPath) {
			return CommonWorkingDirectory.CUSTOMER_REVIEW_IMAGE_DIR.getDirectoryPath(includeServerWorkPath);
		}

		@Override
		public void setFileDetails(FileDetails fileDetails, String fileDetailsKey, Object collectionKey) {
			if (CustomerReviewImageKey.IMAGE_ONE.name().equals(fileDetailsKey)) {
				setImage1Details(fileDetails);		
			} else if (CustomerReviewImageKey.IMAGE_TWO.name().equals(fileDetailsKey)) {
				setImage2Details(fileDetails);		
			}
		}

		@Override
		public FileDetails getFileDetails(String fileDetailsKey, Object collectionKey) {
			if (CustomerReviewImageKey.IMAGE_ONE.name().equals(fileDetailsKey)) {
				return getImage1Details();		
			} else if (CustomerReviewImageKey.IMAGE_TWO.name().equals(fileDetailsKey)) {
				return getImage2Details();		
			}
			return null;
		}
	}
}
