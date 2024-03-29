package com.datvutech.data.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import com.datvutech.data.converter.MonetaryAmountConverter;
import com.datvutech.data.embeddable.Dimension;
import com.datvutech.data.embeddable.Weight;
import com.datvutech.data.usertype.MonetaryAmount;
import com.datvutech.data.usertype.MonetaryAmountUserType;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@TypeDefs({
                @TypeDef(name = "monetary_amount_usd", typeClass = MonetaryAmountUserType.class, parameters = {
                                @Parameter(name = "convertTo", value = "USD")
                })
})
/*
 * Dynamic SQL generation,
 * disable generation of INSERT and UPDATE SQL statements on startup
 */
@DynamicInsert
@DynamicUpdate

@Table(name = "items") /* Customize all table with strategy by implement PhysicalNamingStrategy */
@Entity
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
@ToString
public class Item implements Serializable {

        public Item(String name, Dimension dimension, Double metricWeight) {
                this.name = name;
                this.dimension = dimension;
                this.metricWeight = metricWeight;
        }

        public Item(String name, Dimension dimension, Weight weight) {
                this.name = name;
                this.dimension = dimension;
                this.weight = weight;
        }

        @Id
        @GeneratedValue(generator = "ID_GENERATOR") /* For generate value, use with @Id */
        private Long id;

        @Setter
        @Access(AccessType.PROPERTY) /* Requires getter, setter */
        private String name;

        /* Generated and default property values, usually for the first time */
        @Generated(GenerationTime.INSERT)
        @Convert(converter = MonetaryAmountConverter.class)
        @ColumnDefault("'1.0 USD'")
        @Column(name = "initial_price", nullable = false)
        private MonetaryAmount initialPrice;

        @Type(type = "monetary_amount_usd")
        @Columns(columns = {
                        /* The order is important */
                        @Column(name = "initialprice_amount"),
                        @Column(name = "initialprice_currency", length = 3)
        })
        private MonetaryAmount initialPriceUSD;

        @Setter
        @Column(name = "buy_now_price", length = 64)
        @Convert(converter = MonetaryAmountConverter.class) // Use to specify converter for data type
        private MonetaryAmount buyNowPrice;

        @Columns(columns = {
                        @Column(name = "buynowprice_amount"),
                        @Column(name = "buynowprice_currency", length = 3)
        })
        @Type(type = "monetary_amount_usd")
        private MonetaryAmount buyNowPriceUSD;

        /*
         * Don't allowed INSERT OR UPDATE by statements.
         * Delegate for
         * database
         */
        @Column(name = "last_modified", updatable = false, insertable = false)
        @Generated(GenerationTime.ALWAYS) /* Used with existed triggers */
        private ZonedDateTime lastModified;

        @Formula("(SELECT avg(b.amount) " +
                        "FROM bids b " +
                        "WHERE b.item_id = id)") /* Dynamic load from database */
        private BigDecimal averageBidAmount;

        /*
         * Binary large object (image) , can’t access LOB properties without a database
         * connection
         */
        @Lob
        private Blob imageBlob;

        /*
         * Character large object, can’t access LOB properties without a database
         * connection
         */
        @Lob
        private Clob descriptionClob;

        // @Type(type = "yes_no") -> Map to an other sql type
        @ColumnDefault("false")
        private boolean verified;

        @Formula("(SELECT count(*) " +
                        "FROM bids b " +
                        "WHERE b.item_id = id)") /* Dynamic load from database */
        private long numberOfBids;

        @NotNull
        @Setter
        /* Convert variant measurement units */
        @ColumnTransformer(read = "imperial_weight / 2.20462", write = "? * 2.20462")
        @Column(name = "imperial_weight")
        private Double metricWeight;

        private Dimension dimension;

        private Weight weight;

        /*
         * Persistent collections are always
         * optional—a feature
         */
        @ElementCollection /* Marks collection for a value-type */
        @CollectionTable(name = "images", joinColumns = @JoinColumn(name = "item_id")) /* read-only table */
        @Column(name = "file_name")
        private Set<String> images = new HashSet<>(); /* Should be initialized */

        public void addImage(String fileName) {
                if (fileName == null || fileName.length() < 1) {
                        throw new IllegalArgumentException("file name error");
                }
                images.add(fileName);
        }

        @Getter(AccessLevel.NONE)
        @OneToMany(mappedBy = "item", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
        private Set<Bid> bids = new HashSet<>();

        public void addBid(Bid bid) {
                if (bid == null) {
                        throw new IllegalArgumentException("Can't add null bid");
                } else if (bid.getItem() == null) {
                        throw new NullPointerException("Item is null");
                }
                bids.add(bid);
        }

        // @Transient /* Totally ignore, both persit and load */
        // private Set<Bid> bids = new HashSet<>();

}