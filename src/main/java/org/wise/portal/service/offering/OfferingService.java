/**
 * Copyright (c) 2007 Encore Research Group, University of Toronto
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.wise.portal.service.offering;

import java.util.List;
import java.util.Set;


import org.wise.portal.dao.ObjectNotFoundException;
import org.wise.portal.domain.run.Offering;
import org.wise.portal.domain.run.impl.OfferingParameters;
import org.wise.portal.domain.workgroup.Workgroup;

/**
 * @author Laurel Williams
 * 
 * @version $Id$
 */
public interface OfferingService {

	/**
	 * Gets a list of offerings.
	 * 
	 * @return an offerings <code>List</code>.
	 */
	// @Secured( { "ROLE_USER", "AFTER_ACL_COLLECTION_READ" })
	public List<Offering> getOfferingList();

	// TODO LAW this is wrong but is just to remind me to put appropriate
	// security check here
	// @Secured( { "ROLE_USER", "AFTER_ACL_COLLECTION_READ" })
	/**
	 * Given an offering id, obtains the offering
	 * 
	 * @param id The id of the offering to be retrieved.
	 * @return The offering.
	 * @throws ObjectNotFoundException if an offering with the given id is not found.
	 */
	public Offering getOffering(Long id) throws ObjectNotFoundException;

	/**
	 * Creates a new <code>Offering</code> object in the local data store.
	 * 
	 * @param offeringParameters
	 *            The <code>OfferingParameters</code> that encapsulate
	 *            information needed to create an offering.
	 * @return the offering created.
	 * @throws ObjectNotFoundException
	 *             If the curnit specified to create this offering does not
	 *             exist in the data store.
	 */
	public Offering createOffering(OfferingParameters offeringParameters)
	    throws ObjectNotFoundException;
	
	/**
	 * Returns a set of <code>Workgroup</code> that belong in the the <code>Offering</code>
	 * with the provided offeringId.
	 * 
	 * @param offeringId key to the <code>Offering</code> to look up
	 * @return a Set of Workgroups that belong in the <code>Offering</code>
	 * 
	 * @throws ObjectNotFoundException if an offering with the given id is not found.
	 */
	public Set<Workgroup> getWorkgroupsForOffering(Long offeringId) 
	    throws ObjectNotFoundException;
}